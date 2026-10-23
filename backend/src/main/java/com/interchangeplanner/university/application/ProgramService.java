package com.interchangeplanner.university.application;

import com.interchangeplanner.applications.domain.ApplicationStatus;
import com.interchangeplanner.currency.CurrencyService;
import com.interchangeplanner.exchange.application.CurrencyConverter;
import com.interchangeplanner.exchange.application.ExchangeRateService;
import com.interchangeplanner.shared.error.BusinessRuleException;
import com.interchangeplanner.shared.error.NotFoundException;
import com.interchangeplanner.shared.money.Money;
import com.interchangeplanner.shared.money.MoneyResponse;
import com.interchangeplanner.university.api.UniversityDtos.CompareResponse;
import com.interchangeplanner.university.api.UniversityDtos.IntakeDto;
import com.interchangeplanner.university.api.UniversityDtos.LanguageRequirementDto;
import com.interchangeplanner.university.api.UniversityDtos.ProgramRequest;
import com.interchangeplanner.university.api.UniversityDtos.ProgramResponse;
import com.interchangeplanner.university.domain.LanguageRequirement;
import com.interchangeplanner.university.domain.Program;
import com.interchangeplanner.university.domain.ProgramIntake;
import com.interchangeplanner.university.domain.ProgramRepository;
import com.interchangeplanner.university.domain.UniversityEnums.Rating;
import com.interchangeplanner.university.domain.UniversityEnums.Requirement;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProgramService {

    /**
     * Filtros da listagem; todos opcionais.
     *
     * @param minAdmissionChance chance de admissão mínima (inclusive)
     * @param deadlineBefore     próximo prazo até esta data
     * @param maxSemesterFee     taxa semestral máxima, em EUR (convertida)
     */
    public record Filter(UUID universityId, Boolean chosen, ApplicationStatus applicationStatus, String language,
            Requirement gre, Requirement interview, Requirement nc, Rating minAdmissionChance, LocalDate deadlineBefore,
            BigDecimal maxSemesterFee, String q) {
    }

    static final String COMPARE_CURRENCY = "EUR";

    private final ProgramRepository programs;
    private final UniversityService universities;
    private final ProgramMapper mapper;
    private final CurrencyService currencies;
    private final ExchangeRateService exchange;

    public ProgramService(ProgramRepository programs, UniversityService universities, ProgramMapper mapper,
            CurrencyService currencies, ExchangeRateService exchange) {
        this.programs = programs;
        this.universities = universities;
        this.mapper = mapper;
        this.currencies = currencies;
        this.exchange = exchange;
    }

    @Transactional(readOnly = true)
    public List<ProgramResponse> list(UUID userId, Filter f) {
        List<Program> base = f.universityId() == null
                ? programs.findByUserIdOrderByNameAsc(userId)
                : programs.findByUserIdAndUniversityIdOrderByNameAsc(userId, f.universityId());
        CurrencyConverter converter = exchange.converterFor(userId);
        String q = UniversityService.norm(f.q());
        String language = UniversityService.norm(f.language());
        return mapper.toResponses(userId, base).stream()
                .filter(p -> f.chosen() == null || f.chosen() == p.chosen())
                .filter(p -> f.applicationStatus() == null
                        || (p.application() != null && p.application().status() == f.applicationStatus()))
                .filter(p -> language == null || matchesLanguage(p, language))
                .filter(p -> f.gre() == null || p.gre() == f.gre())
                .filter(p -> f.interview() == null || p.interview() == f.interview())
                .filter(p -> f.nc() == null || p.nc() == f.nc())
                .filter(p -> f.minAdmissionChance() == null
                        || (p.admissionChance() != null && p.admissionChance().compareTo(f.minAdmissionChance()) >= 0))
                .filter(p -> f.deadlineBefore() == null
                        || (p.nextDeadline() != null && !p.nextDeadline().isAfter(f.deadlineBefore())))
                .filter(p -> f.maxSemesterFee() == null || (p.semesterFee() != null
                        && toCompare(p.semesterFee(), converter).compareTo(f.maxSemesterFee()) <= 0))
                .filter(p -> q == null || contains(p.name(), q) || contains(p.area(), q) || contains(p.university().name(), q)
                        || contains(p.university().city(), q))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProgramResponse get(UUID userId, UUID id) {
        return mapper.toResponses(userId, List.of(require(userId, id))).getFirst();
    }

    @Transactional(readOnly = true)
    public Program require(UUID userId, UUID id) {
        return programs.findByIdAndUserId(id, userId).orElseThrow(() -> new NotFoundException("Programa"));
    }

    @Transactional
    public ProgramResponse create(UUID userId, ProgramRequest r) {
        Program p = programs.save(new Program(userId, toData(userId, r)));
        return mapper.toResponses(userId, List.of(p)).getFirst();
    }

    @Transactional
    public ProgramResponse update(UUID userId, UUID id, ProgramRequest r) {
        Program p = require(userId, id);
        p.update(toData(userId, r));
        programs.flush();
        return mapper.toResponses(userId, List.of(p)).getFirst();
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        programs.delete(require(userId, id));
    }

    /** Comparador lado a lado (2–5 programas) com o melhor valor de cada linha destacado. */
    @Transactional(readOnly = true)
    public CompareResponse compare(UUID userId, List<UUID> ids) {
        List<UUID> distinct = ids.stream().distinct().toList();
        if (distinct.size() < 2 || distinct.size() > 5) {
            throw new BusinessRuleException("Compare de 2 a 5 programas.", "ids");
        }
        List<Program> found = programs.findByUserIdAndIdIn(userId, distinct);
        if (found.size() != distinct.size()) {
            throw new NotFoundException("Programa");
        }
        Map<UUID, Integer> order = new LinkedHashMap<>();
        for (int i = 0; i < distinct.size(); i++) {
            order.put(distinct.get(i), i);
        }
        List<ProgramResponse> list = new ArrayList<>(mapper.toResponses(userId, found));
        list.sort(Comparator.comparing(p -> order.get(p.id())));
        CurrencyConverter converter = exchange.converterFor(userId);

        Map<String, List<UUID>> highlights = new LinkedHashMap<>();
        highlights.put("ranking", best(list, p -> p.university().ranking() == null ? null
                : BigDecimal.valueOf(p.university().ranking()), false));
        highlights.put("semesterFee", best(list, p -> toCompare(p.semesterFee(), converter), false));
        highlights.put("nonEuTuition", best(list, p -> toCompare(p.nonEuTuition(), converter), false));
        highlights.put("costOfLiving", best(list, p -> toCompare(p.university().costOfLiving(), converter), false));
        highlights.put("admissionChance", best(list, p -> ordinal(p.admissionChance()), true));
        highlights.put("scholarshipChance", best(list, p -> ordinal(p.scholarshipChance()), true));
        highlights.put("employability", best(list, p -> ordinal(p.university().employability()), true));
        highlights.put("nextDeadline", best(list, p -> p.nextDeadline() == null ? null
                : BigDecimal.valueOf(p.nextDeadline().toEpochDay()), true));
        return new CompareResponse(list, highlights);
    }

    /** Ids com o melhor valor (empates incluídos); vazio se ninguém tem o dado ou todos empatam sem diferença útil. */
    static List<UUID> best(List<ProgramResponse> list, Function<ProgramResponse, BigDecimal> value, boolean higherIsBetter) {
        List<BigDecimal> values = list.stream().map(value).toList();
        BigDecimal best = values.stream().filter(Objects::nonNull)
                .reduce((a, b) -> higherIsBetter ? a.max(b) : a.min(b)).orElse(null);
        if (best == null || values.stream().filter(Objects::nonNull).count() < 2) {
            return List.of();
        }
        List<UUID> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            if (values.get(i) != null && values.get(i).compareTo(best) == 0) {
                result.add(list.get(i).id());
            }
        }
        return result.size() == list.size() ? List.of() : result;
    }

    private static BigDecimal ordinal(Enum<?> e) {
        return e == null ? null : BigDecimal.valueOf(e.ordinal());
    }

    private static BigDecimal toCompare(MoneyResponse m, CurrencyConverter converter) {
        return m == null ? null : converter.convert(Money.of(m.amount(), m.currency()), COMPARE_CURRENCY).amount();
    }

    private static boolean contains(String value, String q) {
        String n = UniversityService.norm(value);
        return n != null && n.contains(q);
    }

    private static boolean matchesLanguage(ProgramResponse p, String language) {
        return contains(p.languageSummary(), language)
                || p.languageRequirements().stream().anyMatch(r -> r.language().equalsIgnoreCase(language));
    }

    private Program.Data toData(UUID userId, ProgramRequest r) {
        universities.require(userId, r.universityId());
        Money fee = money(r.semesterFeeAmount(), r.semesterFeeCurrency(), "semesterFeeCurrency");
        Money tuition = money(r.nonEuTuitionAmount(), r.nonEuTuitionCurrency(), "nonEuTuitionCurrency");
        List<LanguageRequirement> reqs = new ArrayList<>();
        List<LanguageRequirementDto> reqDtos = r.languageRequirements() == null ? List.of() : r.languageRequirements();
        for (int i = 0; i < reqDtos.size(); i++) {
            LanguageRequirementDto d = reqDtos.get(i);
            reqs.add(new LanguageRequirement(d.language(), d.minCefr(), d.testName(), d.minScore(), d.rawText(), i));
        }
        List<ProgramIntake> intakes = new ArrayList<>();
        for (IntakeDto d : r.intakes() == null ? List.<IntakeDto>of() : r.intakes()) {
            validateDayMonth(d.openMonth(), d.openDay(), "intakes.openDay");
            validateDayMonth(d.closeMonth(), d.closeDay(), "intakes.closeDay");
            intakes.add(new ProgramIntake(d.term(), d.openMonth(), d.openDay(), d.closeMonth(), d.closeDay(), d.startMonth()));
        }
        return new Program.Data(r.universityId(), r.name(), r.area(), r.description(), r.officialUrl(), r.degree(),
                r.languageSummary(), r.durationSemesters(), r.gre(), r.aptitudeAssessment(), r.interview(), r.nc(),
                r.applicationChannel(), r.portalName(), r.documentSubmission(), fee, tuition, r.nonEuTuitionPeriod(),
                r.scholarshipAvailable(), r.scholarshipDetails(), r.scholarshipChance(), r.admissionChance(),
                r.applicationDeadlineText(), r.notes(), reqs, intakes);
    }

    private Money money(BigDecimal amount, String currency, String field) {
        if (amount == null) {
            return null;
        }
        String code = currency != null ? currency : COMPARE_CURRENCY;
        if (!currencies.isSupported(code)) {
            throw new BusinessRuleException("Moeda não suportada.", field);
        }
        return Money.of(amount, code);
    }

    private static void validateDayMonth(Integer month, Integer day, String field) {
        if ((month == null) != (day == null)) {
            throw new BusinessRuleException("Informe dia e mês juntos.", field);
        }
        if (month != null && day > Month.of(month).maxLength()) {
            throw new BusinessRuleException("Dia inválido para o mês.", field);
        }
    }
}
