package com.interchangeplanner.university.application;

import com.interchangeplanner.applications.domain.Application;
import com.interchangeplanner.applications.domain.ApplicationRepository;
import com.interchangeplanner.shared.money.MoneyResponse;
import com.interchangeplanner.university.api.UniversityDtos.ApplicationSummary;
import com.interchangeplanner.university.api.UniversityDtos.IntakeResponse;
import com.interchangeplanner.university.api.UniversityDtos.LanguageRequirementDto;
import com.interchangeplanner.university.api.UniversityDtos.NextWindow;
import com.interchangeplanner.university.api.UniversityDtos.ProgramResponse;
import com.interchangeplanner.university.api.UniversityDtos.UniversitySummary;
import com.interchangeplanner.university.domain.Program;
import com.interchangeplanner.university.domain.ProgramIntake;
import com.interchangeplanner.university.domain.University;
import com.interchangeplanner.university.domain.UniversityRepository;
import com.interchangeplanner.user.application.UserClock;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Monta respostas de programas resolvendo universidades e candidaturas em lote (sem N+1). */
@Component
public class ProgramMapper {

    private final UniversityRepository universities;
    private final ApplicationRepository applications;
    private final UserClock userClock;

    public ProgramMapper(UniversityRepository universities, ApplicationRepository applications, UserClock userClock) {
        this.universities = universities;
        this.applications = applications;
        this.userClock = userClock;
    }

    @Transactional(readOnly = true)
    public List<ProgramResponse> toResponses(UUID userId, Collection<Program> programs) {
        if (programs.isEmpty()) {
            return List.of();
        }
        Map<UUID, University> uniIndex = universities.findAllById(programs.stream().map(Program::getUniversityId)
                .distinct().toList()).stream().collect(Collectors.toMap(University::getId, Function.identity()));
        Map<UUID, Application> activeApps = applications.findByUserIdAndProgramIdIn(userId,
                        programs.stream().map(Program::getId).toList()).stream()
                .filter(a -> !a.isArchived())
                .collect(Collectors.toMap(Application::getProgramId, Function.identity(),
                        (a, b) -> a.getIntakeYear() >= b.getIntakeYear() ? a : b));
        LocalDate today = userClock.today(userId);
        return programs.stream().map(p -> toResponse(p, uniIndex.get(p.getUniversityId()), activeApps.get(p.getId()), today))
                .toList();
    }

    public static UniversitySummary summary(University u) {
        return new UniversitySummary(u.getId(), u.getName(), u.getCity(), u.getCountryCode(), u.getRanking(),
                u.getInstitutionType(), u.getNature(), u.costOfLiving() == null ? null : MoneyResponse.of(u.costOfLiving()),
                u.getEmployability(), u.getCoverImageId());
    }

    static IntakeWindowCalculator.Spec spec(ProgramIntake i) {
        return new IntakeWindowCalculator.Spec(i.getTerm(), i.getOpenMonth(), i.getOpenDay(), i.getCloseMonth(),
                i.getCloseDay(), i.getStartMonth());
    }

    static ProgramResponse toResponse(Program p, University u, Application app, LocalDate today) {
        List<IntakeResponse> intakes = p.getIntakes().stream().map(i -> {
            IntakeWindowCalculator.Window w = IntakeWindowCalculator.next(spec(i), today);
            return new IntakeResponse(i.getTerm(), i.getOpenMonth(), i.getOpenDay(), i.getCloseMonth(), i.getCloseDay(),
                    i.getStartMonth(), new NextWindow(w.intakeYear(), w.opensOn(), w.closesOn(), w.startsOn(), w.state()));
        }).toList();
        LocalDate nextDeadline = intakes.stream().map(i -> i.nextWindow().closesOn()).filter(Objects::nonNull)
                .min(Comparator.naturalOrder()).orElse(null);
        return new ProgramResponse(p.getId(), summary(u), p.getName(), p.getArea(), p.getDescription(),
                p.getOfficialUrl(), p.getDegree(), p.getLanguageSummary(), p.getDurationSemesters(), p.getGre(),
                p.getAptitudeAssessment(), p.getInterview(), p.getNc(), p.getApplicationChannel(), p.getPortalName(),
                p.getDocumentSubmission(), p.semesterFee() == null ? null : MoneyResponse.of(p.semesterFee()),
                p.nonEuTuition() == null ? null : MoneyResponse.of(p.nonEuTuition()), p.getNonEuTuitionPeriod(),
                p.getScholarshipAvailable(), p.getScholarshipDetails(), p.getScholarshipChance(), p.getAdmissionChance(),
                p.getApplicationDeadlineText(), p.isChosen(), p.getNotes(),
                p.getLanguageRequirements().stream().map(r -> new LanguageRequirementDto(r.getLanguage(), r.getMinCefr(),
                        r.getTestName(), r.getMinScore(), r.getRawText())).toList(),
                intakes, nextDeadline,
                app == null ? null : new ApplicationSummary(app.getId(), app.getStatus(), app.getStatus().result(),
                        app.getIntakeYear(), app.getTerm()));
    }
}
