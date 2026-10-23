package com.interchangeplanner.importer.application;

import com.interchangeplanner.expense.api.CatalogRequest;
import com.interchangeplanner.expense.api.ExpenseRequest;
import com.interchangeplanner.expense.api.PaymentRequest;
import com.interchangeplanner.expense.application.ExpenseCategoryService;
import com.interchangeplanner.expense.application.ExpenseCycleService;
import com.interchangeplanner.expense.application.ExpenseService;
import com.interchangeplanner.expense.domain.CatalogItem;
import com.interchangeplanner.expense.domain.Expense;
import com.interchangeplanner.expense.domain.ExpenseRepository;
import com.interchangeplanner.importer.api.ImportDtos.CommitRequest;
import com.interchangeplanner.importer.api.ImportDtos.CommitSummary;
import com.interchangeplanner.importer.api.ImportDtos.DuplicateStrategy;
import com.interchangeplanner.importer.api.ImportDtos.ImportPreview;
import com.interchangeplanner.importer.api.ImportDtos.PreviewRow;
import com.interchangeplanner.importer.api.ImportDtos.SheetPreview;
import com.interchangeplanner.importer.api.ImportDtos.SheetSummary;
import com.interchangeplanner.importer.application.ImportModel.ExpenseDraft;
import com.interchangeplanner.importer.application.ImportModel.LuggageDraft;
import com.interchangeplanner.importer.application.ImportModel.Payload;
import com.interchangeplanner.importer.application.ImportModel.ProgramDraft;
import com.interchangeplanner.importer.application.ImportModel.Row;
import com.interchangeplanner.importer.application.ImportModel.RowStatus;
import com.interchangeplanner.importer.application.ImportModel.Sheet;
import com.interchangeplanner.importer.application.ImportModel.UniversityDraft;
import com.interchangeplanner.importer.application.ImportModel.UniversityRowDraft;
import com.interchangeplanner.importer.domain.ImportSession;
import com.interchangeplanner.importer.domain.ImportSessionRepository;
import com.interchangeplanner.luggage.api.LuggageDtos.BagRequest;
import com.interchangeplanner.luggage.api.LuggageDtos.ItemRequest;
import com.interchangeplanner.luggage.application.LuggageService;
import com.interchangeplanner.luggage.domain.LuggageBag;
import com.interchangeplanner.luggage.domain.LuggageBagRepository;
import com.interchangeplanner.luggage.domain.LuggageItem;
import com.interchangeplanner.luggage.domain.LuggageItemRepository;
import com.interchangeplanner.objective.api.AdjustmentRequest;
import com.interchangeplanner.objective.application.AdjustmentService;
import com.interchangeplanner.objective.application.ObjectiveService;
import com.interchangeplanner.objective.domain.Objective;
import com.interchangeplanner.shared.error.ApiException;
import com.interchangeplanner.shared.error.BusinessRuleException;
import com.interchangeplanner.shared.error.ConflictException;
import com.interchangeplanner.shared.error.NotFoundException;
import com.interchangeplanner.university.api.UniversityDtos.ProgramRequest;
import com.interchangeplanner.university.api.UniversityDtos.UniversityRequest;
import com.interchangeplanner.university.application.ProgramService;
import com.interchangeplanner.university.application.UniversityService;
import com.interchangeplanner.university.domain.Program;
import com.interchangeplanner.university.domain.ProgramRepository;
import com.interchangeplanner.university.domain.University;
import com.interchangeplanner.university.domain.UniversityEnums.TuitionPeriod;
import com.interchangeplanner.university.domain.UniversityRepository;
import com.interchangeplanner.user.application.UserClock;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.json.JsonMapper;

/**
 * Importação da planilha em dois passos (plano técnico §3):
 * <ol>
 *   <li><b>Prévia</b>: lê as abas Gastos, Universidades e Mala (Economia, Dashboard e Auxiliares são ignoradas),
 *       normaliza, marca cada linha (válida, aviso, erro, duplicada) e guarda tudo numa sessão.</li>
 *   <li><b>Importar</b>: aplica os rascunhos pelos mesmos serviços das telas (mesmas validações e regras), tudo numa
 *       transação. Duplicados são pulados ou atualizados; uma sessão só é importada uma vez e importar a mesma planilha
 *       de novo com "pular" não cria nada (idempotente).</li>
 * </ol>
 */
@Service
public class ImportService {

    static final long MAX_BYTES = 5L * 1024 * 1024;
    static final Duration SESSION_TTL = Duration.ofHours(2);
    static final String DEFAULT_COLOR = "#64748b";

    private final ImportSessionRepository sessions;
    private final JsonMapper json;
    private final UserClock userClock;
    private final ObjectiveService objectives;
    private final AdjustmentService adjustments;
    private final ExpenseService expenseService;
    private final ExpenseRepository expenses;
    private final ExpenseCategoryService categories;
    private final ExpenseCycleService cycles;
    private final UniversityService universityService;
    private final UniversityRepository universities;
    private final ProgramService programService;
    private final ProgramRepository programs;
    private final LuggageService luggage;
    private final LuggageBagRepository bags;
    private final LuggageItemRepository items;

    public ImportService(ImportSessionRepository sessions, JsonMapper json, UserClock userClock,
            ObjectiveService objectives, AdjustmentService adjustments, ExpenseService expenseService,
            ExpenseRepository expenses, ExpenseCategoryService categories, ExpenseCycleService cycles,
            UniversityService universityService, UniversityRepository universities, ProgramService programService,
            ProgramRepository programs, LuggageService luggage, LuggageBagRepository bags, LuggageItemRepository items) {
        this.sessions = sessions;
        this.json = json;
        this.userClock = userClock;
        this.objectives = objectives;
        this.adjustments = adjustments;
        this.expenseService = expenseService;
        this.expenses = expenses;
        this.categories = categories;
        this.cycles = cycles;
        this.universityService = universityService;
        this.universities = universities;
        this.programService = programService;
        this.programs = programs;
        this.luggage = luggage;
        this.bags = bags;
        this.items = items;
    }

    // ------------------------------------------------------------------ prévia

    @Transactional
    public ImportPreview preview(UUID userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException("Escolha a planilha (.xlsx).", "file");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BusinessRuleException("A planilha deve ter até 5 MB.", "file");
        }
        SheetReader.Workbook wb;
        try {
            wb = SheetReader.read(file.getBytes());
        } catch (IOException | RuntimeException e) {
            throw new BusinessRuleException("Não foi possível ler o arquivo. Envie a planilha no formato .xlsx.", "file");
        }
        var today = userClock.today(userId);
        List<String> catNames = categories.entities(userId).stream().map(CatalogItem::getName).toList();
        List<String> cycleNames = cycles.entities(userId).stream().map(CatalogItem::getName).toList();
        Payload payload = new Payload(
                ExpenseSheetParser.parse(wb.sheet(ExpenseSheetParser.SHEET), catNames, cycleNames, today),
                UniversitySheetParser.parse(wb.sheet(UniversitySheetParser.SHEET)),
                LuggageSheetParser.parse(wb.sheet(LuggageSheetParser.SHEET)),
                ignored(wb));
        if (payload.expenses().isEmpty() && payload.universities().isEmpty() && payload.luggage().isEmpty()) {
            throw new BusinessRuleException("Nenhuma aba reconhecida (Informações dos Gastos, Análise de Universidades, "
                    + "Mala).", "file");
        }
        Instant now = userClock.clock().instant();
        sessions.deleteExpired(userId, now);
        String name = file.getOriginalFilename() == null ? "planilha.xlsx" : file.getOriginalFilename();
        ImportSession session = sessions.save(new ImportSession(userId, name.length() > 200 ? name.substring(0, 200) : name,
                json.writeValueAsString(payload), now.plus(SESSION_TTL)));
        return toPreview(userId, session, payload, null);
    }

    @Transactional(readOnly = true)
    public ImportPreview get(UUID userId, UUID id) {
        ImportSession s = require(userId, id);
        CommitSummary summary = s.getSummary() == null ? null : json.readValue(s.getSummary(), CommitSummary.class);
        return toPreview(userId, s, payload(s), summary);
    }

    private ImportPreview toPreview(UUID userId, ImportSession s, Payload p, CommitSummary summary) {
        Objective objective = objectives.resolve(userId, null).orElse(null);
        Payload marked = s.getStatus() == ImportSession.Status.PREVIEW ? markDuplicates(userId, p, objective) : p;
        List<SheetPreview> sheets = new ArrayList<>();
        if (!marked.expenses().isEmpty()) {
            sheets.add(sheet(Sheet.EXPENSES, "Gastos", marked.expenses()));
        }
        if (!marked.universities().isEmpty()) {
            sheets.add(sheet(Sheet.UNIVERSITIES, "Universidades e programas", marked.universities()));
        }
        if (!marked.luggage().isEmpty()) {
            sheets.add(sheet(Sheet.LUGGAGE, "Mala", marked.luggage()));
        }
        return new ImportPreview(s.getId(), s.getFileName(), s.getStatus().name(), s.getExpiresAt(),
                objective == null ? null : objective.getId(), sheets, p.ignoredSheets(), summary);
    }

    private static SheetPreview sheet(Sheet sheet, String title, List<? extends Row<?>> rows) {
        Map<RowStatus, Long> counts = rows.stream().collect(Collectors.groupingBy(Row::status, Collectors.counting()));
        return new SheetPreview(sheet, title, count(counts, RowStatus.VALID), count(counts, RowStatus.WARNING),
                count(counts, RowStatus.ERROR), count(counts, RowStatus.DUPLICATE),
                rows.stream().map(r -> new PreviewRow(r.row(), r.status(), r.label(), r.messages())).toList());
    }

    private static int count(Map<RowStatus, Long> counts, RowStatus s) {
        return counts.getOrDefault(s, 0L).intValue();
    }

    /** Marca como duplicado o que já existe (gastos no objetivo principal, programas, itens de mala). */
    private Payload markDuplicates(UUID userId, Payload p, Objective objective) {
        Set<String> expenseNames = objective == null ? Set.of()
                : expenses.findByUserIdAndObjectiveId(userId, objective.getId()).stream()
                        .map(e -> ImportText.norm(e.getName())).collect(Collectors.toSet());
        Map<String, University> unis = universityIndex(userId);
        Set<String> programKeys = programs.findByUserIdOrderByNameAsc(userId).stream()
                .map(pr -> pr.getUniversityId() + "|" + ImportText.norm(pr.getName())).collect(Collectors.toSet());
        Map<UUID, String> bagNames = bags.findByUserIdOrderBySortOrderAscCreatedAtAsc(userId).stream()
                .collect(Collectors.toMap(LuggageBag::getId, b -> ImportText.norm(b.getName())));
        Set<String> itemKeys = items.findByUserIdOrderByNameAsc(userId).stream()
                .map(i -> bagNames.getOrDefault(i.getBagId(), "") + "|" + ImportText.norm(i.getName()))
                .collect(Collectors.toSet());

        return new Payload(
                p.expenses().stream().map(r -> r.importable() && expenseNames.contains(ImportText.norm(r.draft().name()))
                        ? r.asDuplicate("Já existe um gasto com esse nome no objetivo.") : r).toList(),
                p.universities().stream().map(r -> {
                    if (!r.importable()) {
                        return r;
                    }
                    University u = unis.get(ImportText.norm(r.draft().university().name()));
                    return u != null && programKeys.contains(u.getId() + "|" + ImportText.norm(r.draft().program().name()))
                            ? r.asDuplicate("Esse programa já está cadastrado nessa universidade.") : r;
                }).toList(),
                p.luggage().stream().map(r -> r.importable() && itemKeys.contains(ImportText.norm(r.draft().bagName())
                        + "|" + ImportText.norm(r.draft().name())) ? r.asDuplicate("Esse item já está nessa mala.") : r)
                        .toList(),
                p.ignoredSheets());
    }

    private static List<String> ignored(SheetReader.Workbook wb) {
        Set<String> used = Set.of(ImportText.norm(ExpenseSheetParser.SHEET), ImportText.norm(UniversitySheetParser.SHEET),
                ImportText.norm(LuggageSheetParser.SHEET));
        return wb.sheets().keySet().stream().filter(k -> !used.contains(k)).toList();
    }

    // ------------------------------------------------------------------ importar

    @Transactional
    public ImportPreview commit(UUID userId, UUID id, CommitRequest r) {
        ImportSession session = require(userId, id);
        Instant now = userClock.clock().instant();
        if (session.getStatus() == ImportSession.Status.COMMITTED) {
            throw new ConflictException("Esta planilha já foi importada. Envie de novo para outra importação.", null);
        }
        if (session.isExpired(now)) {
            throw new BusinessRuleException("A prévia expirou. Envie a planilha de novo.");
        }
        Payload p = payload(session);
        Set<Sheet> sheets = r == null || r.sheets() == null || r.sheets().isEmpty() ? EnumSet.allOf(Sheet.class)
                : EnumSet.copyOf(r.sheets());
        DuplicateStrategy strategy = r == null || r.duplicates() == null ? DuplicateStrategy.SKIP : r.duplicates();
        List<SheetSummary> summaries = new ArrayList<>();
        List<String> notes = new ArrayList<>();

        if (sheets.contains(Sheet.EXPENSES) && !p.expenses().isEmpty()) {
            Objective objective = (r == null || r.objectiveId() == null)
                    ? objectives.resolve(userId, null).orElseThrow(
                            () -> new BusinessRuleException("Crie um objetivo antes de importar os gastos.", "objectiveId"))
                    : objectives.requireOwned(userId, r.objectiveId());
            boolean sperrkonto = r == null || r.sperrkontoAdjustment() == null || r.sperrkontoAdjustment();
            summaries.add(importExpenses(userId, objective, p.expenses(), strategy, sperrkonto, notes));
        }
        if (sheets.contains(Sheet.UNIVERSITIES) && !p.universities().isEmpty()) {
            summaries.add(importUniversities(userId, p.universities(), strategy, notes));
        }
        if (sheets.contains(Sheet.LUGGAGE) && !p.luggage().isEmpty()) {
            summaries.add(importLuggage(userId, p.luggage(), strategy, notes));
        }
        CommitSummary summary = new CommitSummary(summaries, notes);
        session.commit(json.writeValueAsString(summary), now);
        return toPreview(userId, session, p, summary);
    }

    private SheetSummary importExpenses(UUID userId, Objective objective, List<Row<ExpenseDraft>> rows,
            DuplicateStrategy strategy, boolean sperrkonto, List<String> notes) {
        Map<String, UUID> cats = catalog(categories.entities(userId));
        Map<String, UUID> cyc = catalog(cycles.entities(userId));
        Map<String, Expense> existing = expenses.findByUserIdAndObjectiveId(userId, objective.getId()).stream()
                .collect(Collectors.toMap(e -> ImportText.norm(e.getName()), Function.identity(), (a, b) -> a));
        int created = 0;
        int updated = 0;
        int skipped = 0;
        for (Row<ExpenseDraft> row : rows) {
            if (!row.importable()) {
                continue;
            }
            ExpenseDraft d = row.draft();
            try {
                UUID categoryId = ensure(cats, d.category() == null ? "Outros" : d.category(), true, userId, notes);
                UUID cycleId = d.cycle() == null ? null : ensure(cyc, d.cycle(), false, userId, notes);
                ExpenseRequest req = new ExpenseRequest(objective.getId(), categoryId, cycleId, d.name(), d.description(),
                        d.currency(), d.unitPrice(), d.quantity(), d.recurrence(), null, false, d.required(), d.dueDate(),
                        null, d.notes());
                Expense dup = existing.get(ImportText.norm(d.name()));
                UUID expenseId;
                if (dup != null) {
                    if (strategy == DuplicateStrategy.SKIP) {
                        skipped++;
                        continue;
                    }
                    expenseId = expenseService.update(userId, dup.getId(), req).expense().id();
                    updated++;
                    if (!expenseService.payments(userId, expenseId).isEmpty()) {
                        continue;
                    }
                } else {
                    expenseId = expenseService.create(userId, req).expense().id();
                    existing.put(ImportText.norm(d.name()), expenses.findById(expenseId).orElseThrow());
                    created++;
                }
                if (d.paidOn() != null) {
                    expenseService.addPayment(userId, expenseId,
                            new PaymentRequest(d.paidOn(), d.paidAmount(), d.currency(), null, "Importado da planilha"));
                }
            } catch (ApiException e) {
                throw rowError("Gastos", row, e);
            }
        }
        if (sperrkonto) {
            rows.stream().filter(Row::importable)
                    .filter(row -> ImportText.norm(row.draft().name()).contains("sperrkonto"))
                    .findFirst()
                    .ifPresent(row -> {
                        boolean exists = adjustments.list(userId, objective.getId()).stream()
                                .anyMatch(a -> ImportText.norm(a.label()).contains("sperrkonto"));
                        if (!exists) {
                            adjustments.create(userId, objective.getId(), new AdjustmentRequest(
                                    "Sperrkonto (conta bloqueada)", row.draft().unitPrice().negate(), row.draft().currency()));
                            notes.add("Ajuste \"Sperrkonto\" de −" + row.draft().unitPrice() + " " + row.draft().currency()
                                    + " criado no objetivo (descontado uma única vez).");
                        }
                    });
        }
        return new SheetSummary(Sheet.EXPENSES, created, updated, skipped);
    }

    private SheetSummary importUniversities(UUID userId, List<Row<UniversityRowDraft>> rows, DuplicateStrategy strategy,
            List<String> notes) {
        Map<String, University> unis = universityIndex(userId);
        Set<UUID> touched = new HashSet<>();
        int newUniversities = 0;
        int created = 0;
        int updated = 0;
        int skipped = 0;
        for (Row<UniversityRowDraft> row : rows) {
            if (!row.importable()) {
                continue;
            }
            UniversityDraft ud = row.draft().university();
            ProgramDraft pd = row.draft().program();
            try {
                University u = unis.get(ImportText.norm(ud.name()));
                UUID universityId;
                if (u == null) {
                    universityId = universityService.create(userId, universityRequest(ud)).id();
                    unis.put(ImportText.norm(ud.name()), universities.findById(universityId).orElseThrow());
                    touched.add(universityId);
                    newUniversities++;
                } else {
                    universityId = u.getId();
                    if (strategy == DuplicateStrategy.UPDATE && touched.add(universityId)) {
                        universityService.update(userId, universityId, universityRequest(ud));
                    }
                }
                Program existing = programs.findByUserIdAndUniversityIdOrderByNameAsc(userId, universityId).stream()
                        .filter(pr -> ImportText.norm(pr.getName()).equals(ImportText.norm(pd.name())))
                        .findFirst().orElse(null);
                ProgramRequest req = programRequest(universityId, pd);
                if (existing == null) {
                    programService.create(userId, req);
                    created++;
                } else if (strategy == DuplicateStrategy.UPDATE) {
                    programService.update(userId, existing.getId(), req);
                    updated++;
                } else {
                    skipped++;
                }
            } catch (ApiException e) {
                throw rowError("Universidades", row, e);
            }
        }
        if (newUniversities > 0) {
            notes.add(newUniversities + (newUniversities == 1 ? " universidade criada." : " universidades criadas."));
        }
        return new SheetSummary(Sheet.UNIVERSITIES, created, updated, skipped);
    }

    private SheetSummary importLuggage(UUID userId, List<Row<LuggageDraft>> rows, DuplicateStrategy strategy,
            List<String> notes) {
        Map<String, LuggageBag> bagIndex = bags.findByUserIdOrderBySortOrderAscCreatedAtAsc(userId).stream()
                .collect(Collectors.toMap(b -> ImportText.norm(b.getName()), Function.identity(), (a, b) -> a));
        Map<String, LuggageItem> itemIndex = new HashMap<>();
        Map<UUID, String> bagNames = new HashMap<>();
        bagIndex.values().forEach(b -> bagNames.put(b.getId(), ImportText.norm(b.getName())));
        items.findByUserIdOrderByNameAsc(userId).forEach(i -> itemIndex.putIfAbsent(
                bagNames.getOrDefault(i.getBagId(), "") + "|" + ImportText.norm(i.getName()), i));
        int created = 0;
        int updated = 0;
        int skipped = 0;
        for (Row<LuggageDraft> row : rows) {
            if (!row.importable()) {
                continue;
            }
            LuggageDraft d = row.draft();
            try {
                UUID bagId = null;
                if (d.bagName() != null) {
                    LuggageBag bag = bagIndex.get(ImportText.norm(d.bagName()));
                    if (bag == null) {
                        bagId = luggage.createBag(userId, new BagRequest(d.bagName(), d.bagType(), null)).id();
                        bag = bags.findById(bagId).orElseThrow();
                        bagIndex.put(ImportText.norm(d.bagName()), bag);
                        notes.add("Mala \"" + d.bagName() + "\" criada (defina o limite de peso).");
                    }
                    bagId = bag.getId();
                }
                ItemRequest req = new ItemRequest(bagId, d.name(), d.category(), d.quantity(), d.unitWeightG(), null,
                        null, null, d.notes());
                LuggageItem existing = itemIndex.get(ImportText.norm(d.bagName()) + "|" + ImportText.norm(d.name()));
                if (existing == null) {
                    luggage.createItem(userId, req);
                    created++;
                } else if (strategy == DuplicateStrategy.UPDATE) {
                    luggage.updateItem(userId, existing.getId(), new ItemRequest(bagId, d.name(), d.category(),
                            d.quantity(), d.unitWeightG(), existing.getPriority(), existing.isRequired(),
                            existing.isPacked(), d.notes()));
                    updated++;
                } else {
                    skipped++;
                }
            } catch (ApiException e) {
                throw rowError("Mala", row, e);
            }
        }
        return new SheetSummary(Sheet.LUGGAGE, created, updated, skipped);
    }

    // ------------------------------------------------------------------ apoio

    private UUID ensure(Map<String, UUID> catalog, String name, boolean category, UUID userId, List<String> notes) {
        UUID id = catalog.get(ImportText.norm(name));
        if (id != null) {
            return id;
        }
        String clean = name.length() > 60 ? name.substring(0, 60) : name;
        CatalogRequest req = new CatalogRequest(clean, DEFAULT_COLOR, category ? "tag" : "circle", null, null);
        id = (category ? categories.create(userId, req) : cycles.create(userId, req)).id();
        catalog.put(ImportText.norm(name), id);
        notes.add((category ? "Categoria" : "Ciclo") + " \"" + clean + "\" criad" + (category ? "a." : "o."));
        return id;
    }

    private static Map<String, UUID> catalog(List<? extends CatalogItem> list) {
        Map<String, UUID> map = new HashMap<>();
        list.forEach(c -> map.putIfAbsent(ImportText.norm(c.getName()), c.getId()));
        return map;
    }

    private Map<String, University> universityIndex(UUID userId) {
        return universities.findByUserIdOrderByNameAsc(userId).stream()
                .collect(Collectors.toMap(u -> ImportText.norm(u.getName()), Function.identity(), (a, b) -> a, HashMap::new));
    }

    private static UniversityRequest universityRequest(UniversityDraft u) {
        return new UniversityRequest(u.name(), u.ranking(), u.institutionType(), u.nature(), u.city(), u.region(),
                u.countryCode(), u.population(), null, null, u.costOfLiving(), u.costOfLiving() == null ? null : "EUR",
                u.employability(), null, null);
    }

    private static ProgramRequest programRequest(UUID universityId, ProgramDraft p) {
        return new ProgramRequest(universityId, p.name(), p.area(), null, null, p.degree(), p.languageSummary(),
                p.durationSemesters(), p.gre(), p.aptitudeAssessment(), p.interview(), p.nc(), p.applicationChannel(),
                p.portalName(), p.documentSubmission(), p.semesterFee(), p.semesterFee() == null ? null : "EUR",
                p.nonEuTuition(), p.nonEuTuition() == null ? null : "EUR",
                p.nonEuTuition() == null ? null : TuitionPeriod.SEMESTER,
                p.scholarshipAvailable(), p.scholarshipDetails(), p.scholarshipChance(), p.admissionChance(),
                p.applicationDeadlineText(), null, p.languageRequirements(), p.intakes());
    }

    private static BusinessRuleException rowError(String sheet, Row<?> row, ApiException e) {
        return new BusinessRuleException(sheet + ", linha " + row.row() + " (" + row.label() + "): " + e.getMessage());
    }

    private ImportSession require(UUID userId, UUID id) {
        return sessions.findByIdAndUserId(id, userId).orElseThrow(() -> new NotFoundException("Importação"));
    }

    private Payload payload(ImportSession s) {
        return json.readValue(s.getPayload(), Payload.class);
    }
}
