package com.interchangeplanner.applications.application;

import com.interchangeplanner.applications.api.ApplicationDtos.ApplicationCreateRequest;
import com.interchangeplanner.applications.api.ApplicationDtos.ApplicationResponse;
import com.interchangeplanner.applications.api.ApplicationDtos.ApplicationUpdateRequest;
import com.interchangeplanner.applications.api.ApplicationDtos.CycleWindow;
import com.interchangeplanner.applications.api.ApplicationDtos.ProgramRef;
import com.interchangeplanner.applications.api.ApplicationDtos.RequestRequest;
import com.interchangeplanner.applications.api.ApplicationDtos.RequestResponse;
import com.interchangeplanner.applications.api.ApplicationDtos.StatusChangeRequest;
import com.interchangeplanner.applications.api.ApplicationDtos.StatusHistoryResponse;
import com.interchangeplanner.applications.domain.Application;
import com.interchangeplanner.applications.domain.ApplicationRepository;
import com.interchangeplanner.applications.domain.ApplicationRequest;
import com.interchangeplanner.applications.domain.ApplicationRequestRepository;
import com.interchangeplanner.applications.domain.ApplicationStatus;
import com.interchangeplanner.applications.domain.ApplicationStatusHistory;
import com.interchangeplanner.applications.domain.ApplicationStatusHistoryRepository;
import com.interchangeplanner.objective.application.ObjectiveService;
import com.interchangeplanner.objective.domain.Objective;
import com.interchangeplanner.shared.error.BusinessRuleException;
import com.interchangeplanner.shared.error.ConflictException;
import com.interchangeplanner.shared.error.NotFoundException;
import com.interchangeplanner.university.api.UniversityDtos.ProgramResponse;
import com.interchangeplanner.university.application.IntakeWindowCalculator;
import com.interchangeplanner.university.application.ProgramService;
import com.interchangeplanner.university.domain.Program;
import com.interchangeplanner.university.domain.ProgramIntake;
import com.interchangeplanner.university.domain.ProgramRepository;
import com.interchangeplanner.university.domain.University;
import com.interchangeplanner.university.domain.UniversityEnums.Term;
import com.interchangeplanner.university.domain.UniversityRepository;
import com.interchangeplanner.user.application.UserClock;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Candidaturas: criação (inclusive ao marcar um programa como "Escolhida"), fluxo de status com histórico,
 * estimativa de resposta e solicitações da universidade.
 *
 * <p>Automação de "Solicitado item": registrar uma solicitação pendente com a candidatura em análise
 * (documentos em análise ou aguardando resultado) muda o status para ITEM_REQUESTED; quando a última pendente
 * é atendida (ou removida), volta para DOCUMENTS_UNDER_REVIEW. Ambas as mudanças entram no histórico.</p>
 */
@Service
public class ApplicationService {

    private final ApplicationRepository applications;
    private final ApplicationStatusHistoryRepository history;
    private final ApplicationRequestRepository requests;
    private final ProgramRepository programs;
    private final UniversityRepository universities;
    private final ProgramService programService;
    private final ObjectiveService objectives;
    private final Clock clock;
    private final UserClock userClock;

    public ApplicationService(ApplicationRepository applications, ApplicationStatusHistoryRepository history,
            ApplicationRequestRepository requests, ProgramRepository programs, UniversityRepository universities,
            ProgramService programService, ObjectiveService objectives, UserClock userClock) {
        this.applications = applications;
        this.history = history;
        this.requests = requests;
        this.programs = programs;
        this.universities = universities;
        this.programService = programService;
        this.objectives = objectives;
        this.clock = userClock.clock();
        this.userClock = userClock;
    }

    // ------------------------------------------------------------------ leitura

    @Transactional(readOnly = true)
    public List<ApplicationResponse> list(UUID userId, ApplicationStatus status, UUID objectiveId, boolean includeArchived) {
        List<Application> list = applications.findByUserId(userId).stream()
                .filter(a -> includeArchived || !a.isArchived())
                .filter(a -> status == null || a.getStatus() == status)
                .filter(a -> objectiveId == null || objectiveId.equals(a.getObjectiveId()))
                .sorted(Comparator.comparing(Application::getStatus).thenComparing(Application::getDeadline,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        return toResponses(userId, list);
    }

    @Transactional(readOnly = true)
    public ApplicationResponse get(UUID userId, UUID id) {
        return toResponses(userId, List.of(require(userId, id))).getFirst();
    }

    @Transactional(readOnly = true)
    public List<StatusHistoryResponse> history(UUID userId, UUID id) {
        require(userId, id);
        return history.findByApplicationIdOrderByChangedOnAscCreatedAtAsc(id).stream()
                .map(h -> new StatusHistoryResponse(h.getId(), h.getStatus(), h.getChangedOn(), h.getNote()))
                .toList();
    }

    // ------------------------------------------------------------------ criação / "Escolhida"

    @Transactional
    public ApplicationResponse create(UUID userId, ApplicationCreateRequest r) {
        Program program = programService.require(userId, r.programId());
        Application app = openFor(userId, program, r.objectiveId(), r.intakeYear(), r.term(), r.deadline(), r.notes(),
                false);
        return toResponses(userId, List.of(app)).getFirst();
    }

    /**
     * Marca/desmarca o programa como "Escolhida". Marcar cria a candidatura do próximo ciclo (ou reativa uma
     * arquivada); desmarcar arquiva as candidaturas ativas, sem apagar o histórico.
     */
    @Transactional
    public ProgramResponse choose(UUID userId, UUID programId, boolean chosen) {
        Program program = programService.require(userId, programId);
        program.setChosen(chosen);
        if (chosen) {
            if (applications.findByUserIdAndProgramIdAndArchivedAtIsNull(userId, programId).isEmpty()) {
                openFor(userId, program, null, null, null, null, null, true);
            }
        } else {
            applications.findByUserIdAndProgramIdAndArchivedAtIsNull(userId, programId)
                    .forEach(a -> a.archive(clock.instant()));
        }
        programs.flush();
        return programService.get(userId, programId);
    }

    private Application openFor(UUID userId, Program program, UUID objectiveId, Integer intakeYear, Term term,
            LocalDate deadline, String notes, boolean reactivateArchived) {
        LocalDate today = userClock.today(userId);
        Optional<ProgramIntake> intake = term == null ? program.getIntakes().stream().findFirst()
                : program.getIntakes().stream().filter(i -> i.getTerm() == term).findFirst();
        IntakeWindowCalculator.Window window = intake.map(i -> intakeYear == null
                        ? IntakeWindowCalculator.next(spec(i), today)
                        : IntakeWindowCalculator.forYear(spec(i), intakeYear, today))
                .orElse(null);
        Term cycleTerm = term != null ? term : intake.map(ProgramIntake::getTerm).orElse(Term.WINTER);
        int cycleYear = intakeYear != null ? intakeYear : window != null ? window.intakeYear() : defaultYear(cycleTerm, today);

        Optional<Application> existing = applications.findByUserIdAndProgramIdAndIntakeYearAndTerm(userId,
                program.getId(), cycleYear, cycleTerm);
        if (existing.isPresent()) {
            Application app = existing.get();
            if (app.isArchived() || reactivateArchived) {
                app.unarchive();
                program.setChosen(true);
                return app;
            }
            throw new ConflictException("Já existe candidatura para este programa neste ciclo.", "programId");
        }
        UUID objective = objectiveId != null ? objectives.requireOwned(userId, objectiveId).getId()
                : objectives.resolve(userId, null).map(Objective::getId).orElse(null);
        LocalDate cycleDeadline = deadline != null ? deadline : window != null ? window.closesOn() : null;
        Application app = applications.save(new Application(userId, program.getId(), new Application.Data(objective,
                cycleYear, cycleTerm, null, cycleDeadline, null, null, null, null, null, null, notes)));
        history.save(new ApplicationStatusHistory(app.getId(), ApplicationStatus.AWAITING_OPENING, today,
                "Candidatura criada", clock.instant()));
        program.setChosen(true);
        return app;
    }

    private static int defaultYear(Term term, LocalDate today) {
        int startMonth = term == Term.SUMMER ? 4 : 10;
        return today.getMonthValue() < startMonth ? today.getYear() : today.getYear() + 1;
    }

    // ------------------------------------------------------------------ edição e status

    @Transactional
    public ApplicationResponse update(UUID userId, UUID id, ApplicationUpdateRequest r) {
        Application app = require(userId, id);
        validateEstimate(r.responseMinDate(), r.responseLikelyDate(), r.responseMaxDate());
        UUID objective = r.objectiveId() == null ? null : objectives.requireOwned(userId, r.objectiveId()).getId();
        if ((r.intakeYear() != app.getIntakeYear() || r.term() != app.getTerm())
                && applications.findByUserIdAndProgramIdAndIntakeYearAndTerm(userId, app.getProgramId(), r.intakeYear(),
                        r.term()).isPresent()) {
            throw new ConflictException("Já existe candidatura para este programa neste ciclo.", "intakeYear");
        }
        app.update(new Application.Data(objective, r.intakeYear(), r.term(), r.submittedAt(), r.deadline(),
                r.responseMinDate(), r.responseLikelyDate(), r.responseMaxDate(), r.responseConfidence(),
                r.responseEstimateBasis(), r.actualResponseDate(), r.notes()));
        return toResponses(userId, List.of(app)).getFirst();
    }

    @Transactional
    public ApplicationResponse changeStatus(UUID userId, UUID id, StatusChangeRequest r) {
        Application app = require(userId, id);
        LocalDate today = userClock.today(userId);
        LocalDate on = r.changedOn() != null ? r.changedOn() : today;
        if (on.isAfter(today)) {
            throw new BusinessRuleException("A data da mudança não pode estar no futuro.", "changedOn");
        }
        record(app, r.status(), on, r.note());
        if (r.status().isFinal() && app.getActualResponseDate() == null && r.status() != ApplicationStatus.ENROLLED) {
            app.update(new Application.Data(app.getObjectiveId(), app.getIntakeYear(), app.getTerm(), app.getSubmittedAt(),
                    app.getDeadline(), app.getResponseMinDate(), app.getResponseLikelyDate(), app.getResponseMaxDate(),
                    app.getResponseConfidence(), app.getResponseEstimateBasis(), on, app.getNotes()));
        }
        return toResponses(userId, List.of(app)).getFirst();
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        Application app = require(userId, id);
        UUID programId = app.getProgramId();
        applications.delete(app);
        applications.flush();
        if (applications.findByUserIdAndProgramIdAndArchivedAtIsNull(userId, programId).isEmpty()) {
            programs.findByIdAndUserId(programId, userId).ifPresent(p -> p.setChosen(false));
        }
    }

    // ------------------------------------------------------------------ solicitações

    @Transactional(readOnly = true)
    public List<RequestResponse> requests(UUID userId, UUID applicationId) {
        require(userId, applicationId);
        LocalDate today = userClock.today(userId);
        return requests.findByUserIdAndApplicationIdOrderByRequestedOnAscCreatedAtAsc(userId, applicationId).stream()
                .map(q -> toResponse(q, today)).toList();
    }

    @Transactional
    public RequestResponse createRequest(UUID userId, UUID applicationId, RequestRequest r) {
        Application app = require(userId, applicationId);
        LocalDate today = userClock.today(userId);
        ApplicationRequest q = requests.save(new ApplicationRequest(userId, applicationId, r.type(), r.description(),
                r.requestedOn() != null ? r.requestedOn() : today, r.dueDate(), r.fulfilledOn()));
        validateRequest(q);
        syncItemRequested(app, today, "Solicitação: " + q.getDescription());
        return toResponse(q, today);
    }

    @Transactional
    public RequestResponse updateRequest(UUID userId, UUID applicationId, UUID requestId, RequestRequest r) {
        Application app = require(userId, applicationId);
        ApplicationRequest q = requireRequest(userId, applicationId, requestId);
        LocalDate today = userClock.today(userId);
        q.update(r.type(), r.description(), r.requestedOn() != null ? r.requestedOn() : q.getRequestedOn(), r.dueDate(),
                r.fulfilledOn());
        validateRequest(q);
        requests.flush();
        syncItemRequested(app, today, q.isPending() ? "Solicitação: " + q.getDescription() : "Solicitações atendidas");
        return toResponse(q, today);
    }

    @Transactional
    public void deleteRequest(UUID userId, UUID applicationId, UUID requestId) {
        Application app = require(userId, applicationId);
        requests.delete(requireRequest(userId, applicationId, requestId));
        requests.flush();
        syncItemRequested(app, userClock.today(userId), "Solicitações atendidas");
    }

    private void syncItemRequested(Application app, LocalDate today, String note) {
        boolean pending = requests.findByUserIdAndApplicationIdOrderByRequestedOnAscCreatedAtAsc(app.getUserId(), app.getId())
                .stream().anyMatch(ApplicationRequest::isPending);
        ApplicationStatus s = app.getStatus();
        if (pending && (s == ApplicationStatus.DOCUMENTS_UNDER_REVIEW || s == ApplicationStatus.AWAITING_RESULT)) {
            record(app, ApplicationStatus.ITEM_REQUESTED, today, note);
        } else if (!pending && s == ApplicationStatus.ITEM_REQUESTED) {
            record(app, ApplicationStatus.DOCUMENTS_UNDER_REVIEW, today, note);
        }
    }

    private void record(Application app, ApplicationStatus status, LocalDate on, String note) {
        app.changeStatus(status);
        history.save(new ApplicationStatusHistory(app.getId(), status, on, note, clock.instant()));
    }

    // ------------------------------------------------------------------ apoio

    static void validateEstimate(LocalDate min, LocalDate likely, LocalDate max) {
        if (min != null && likely != null && min.isAfter(likely)) {
            throw new BusinessRuleException("A data mínima deve ser antes da provável.", "responseLikelyDate");
        }
        if (likely != null && max != null && likely.isAfter(max)) {
            throw new BusinessRuleException("A data provável deve ser antes da máxima.", "responseMaxDate");
        }
        if (min != null && max != null && min.isAfter(max)) {
            throw new BusinessRuleException("A data mínima deve ser antes da máxima.", "responseMaxDate");
        }
    }

    private static void validateRequest(ApplicationRequest q) {
        if (q.getDueDate() != null && q.getDueDate().isBefore(q.getRequestedOn())) {
            throw new BusinessRuleException("O prazo deve ser depois da data do pedido.", "dueDate");
        }
        if (q.getFulfilledOn() != null && q.getFulfilledOn().isBefore(q.getRequestedOn())) {
            throw new BusinessRuleException("O atendimento deve ser depois da data do pedido.", "fulfilledOn");
        }
    }

    private Application require(UUID userId, UUID id) {
        return applications.findByIdAndUserId(id, userId).orElseThrow(() -> new NotFoundException("Candidatura"));
    }

    private ApplicationRequest requireRequest(UUID userId, UUID applicationId, UUID id) {
        return requests.findByIdAndUserIdAndApplicationId(id, userId, applicationId)
                .orElseThrow(() -> new NotFoundException("Solicitação"));
    }

    private static IntakeWindowCalculator.Spec spec(ProgramIntake i) {
        return new IntakeWindowCalculator.Spec(i.getTerm(), i.getOpenMonth(), i.getOpenDay(), i.getCloseMonth(),
                i.getCloseDay(), i.getStartMonth());
    }

    private static RequestResponse toResponse(ApplicationRequest q, LocalDate today) {
        return new RequestResponse(q.getId(), q.getType(), q.getDescription(), q.getRequestedOn(), q.getDueDate(),
                q.getFulfilledOn(), q.isPending(), q.isPending() && q.getDueDate() != null && q.getDueDate().isBefore(today));
    }

    /** Respostas em lote: programas, universidades e solicitações numa query cada. */
    @Transactional(readOnly = true)
    public List<ApplicationResponse> toResponses(UUID userId, List<Application> list) {
        if (list.isEmpty()) {
            return List.of();
        }
        Map<UUID, Program> programIndex = programs.findByUserIdAndIdIn(userId,
                        list.stream().map(Application::getProgramId).distinct().toList()).stream()
                .collect(Collectors.toMap(Program::getId, Function.identity()));
        Map<UUID, University> uniIndex = universities.findAllById(programIndex.values().stream()
                .map(Program::getUniversityId).distinct().toList()).stream()
                .collect(Collectors.toMap(University::getId, Function.identity()));
        Map<UUID, List<ApplicationRequest>> reqIndex = requests.findByUserIdAndApplicationIdIn(userId,
                list.stream().map(Application::getId).toList()).stream()
                .collect(Collectors.groupingBy(ApplicationRequest::getApplicationId));
        LocalDate today = userClock.today(userId);
        return list.stream().map(a -> {
            Program p = programIndex.get(a.getProgramId());
            University u = p == null ? null : uniIndex.get(p.getUniversityId());
            CycleWindow window = p == null ? null : p.getIntakes().stream().filter(i -> i.getTerm() == a.getTerm())
                    .findFirst()
                    .map(i -> IntakeWindowCalculator.forYear(spec(i), a.getIntakeYear(), today))
                    .map(w -> new CycleWindow(w.opensOn(), w.closesOn(), w.startsOn(), w.state()))
                    .orElse(null);
            List<ApplicationRequest> pending = reqIndex.getOrDefault(a.getId(), List.of()).stream()
                    .filter(ApplicationRequest::isPending).toList();
            LocalDate nextDue = pending.stream().map(ApplicationRequest::getDueDate).filter(Objects::nonNull)
                    .min(Comparator.naturalOrder()).orElse(null);
            return new ApplicationResponse(a.getId(),
                    new ProgramRef(a.getProgramId(), p == null ? "" : p.getName(), u == null ? null : u.getId(),
                            u == null ? "" : u.getName(), u == null ? null : u.getCity(), u == null ? null : u.getCountryCode()),
                    a.getObjectiveId(), a.getStatus(), a.getStatus().result(), a.getIntakeYear(), a.getTerm(),
                    a.isArchived(), a.getSubmittedAt(), a.getDeadline(), a.getResponseMinDate(), a.getResponseLikelyDate(),
                    a.getResponseMaxDate(), a.getResponseConfidence(), a.getResponseEstimateBasis(),
                    a.getActualResponseDate(), a.getNotes(), window, pending.size(), nextDue);
        }).toList();
    }
}
