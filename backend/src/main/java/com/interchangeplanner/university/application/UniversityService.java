package com.interchangeplanner.university.application;

import com.interchangeplanner.currency.CurrencyService;
import com.interchangeplanner.shared.error.BusinessRuleException;
import com.interchangeplanner.shared.error.NotFoundException;
import com.interchangeplanner.shared.money.Money;
import com.interchangeplanner.shared.money.MoneyResponse;
import com.interchangeplanner.university.api.UniversityDtos.UniversityDetailResponse;
import com.interchangeplanner.university.api.UniversityDtos.UniversityRequest;
import com.interchangeplanner.university.api.UniversityDtos.UniversityResponse;
import com.interchangeplanner.university.domain.Program;
import com.interchangeplanner.university.domain.ProgramRepository;
import com.interchangeplanner.university.domain.University;
import com.interchangeplanner.university.domain.UniversityEnums.InstitutionType;
import com.interchangeplanner.university.domain.UniversityEnums.Nature;
import com.interchangeplanner.university.domain.UniversityRepository;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UniversityService {

    public record Filter(String countryCode, String city, InstitutionType type, Nature nature, String q) {
    }

    private final UniversityRepository universities;
    private final ProgramRepository programs;
    private final ProgramMapper mapper;
    private final CurrencyService currencies;

    public UniversityService(UniversityRepository universities, ProgramRepository programs, ProgramMapper mapper,
            CurrencyService currencies) {
        this.universities = universities;
        this.programs = programs;
        this.mapper = mapper;
        this.currencies = currencies;
    }

    @Transactional(readOnly = true)
    public List<UniversityResponse> list(UUID userId, Filter f) {
        String q = norm(f.q());
        String city = norm(f.city());
        Map<UUID, List<Program>> byUni = programs.findByUserIdOrderByNameAsc(userId).stream()
                .collect(Collectors.groupingBy(Program::getUniversityId));
        return universities.findByUserIdOrderByNameAsc(userId).stream()
                .filter(u -> f.countryCode() == null || f.countryCode().equals(u.getCountryCode()))
                .filter(u -> city == null || (u.getCity() != null && norm(u.getCity()).contains(city)))
                .filter(u -> f.type() == null || f.type() == u.getInstitutionType())
                .filter(u -> f.nature() == null || f.nature() == u.getNature())
                .filter(u -> q == null || norm(u.getName()).contains(q)
                        || byUni.getOrDefault(u.getId(), List.of()).stream().anyMatch(p -> norm(p.getName()).contains(q)))
                .map(u -> toResponse(u, byUni.getOrDefault(u.getId(), List.of())))
                .toList();
    }

    @Transactional(readOnly = true)
    public UniversityDetailResponse get(UUID userId, UUID id) {
        University u = require(userId, id);
        List<Program> list = programs.findByUserIdAndUniversityIdOrderByNameAsc(userId, id);
        return new UniversityDetailResponse(toResponse(u, list), mapper.toResponses(userId, list));
    }

    @Transactional(readOnly = true)
    public University require(UUID userId, UUID id) {
        return universities.findByIdAndUserId(id, userId).orElseThrow(() -> new NotFoundException("Universidade"));
    }

    @Transactional
    public UniversityResponse create(UUID userId, UniversityRequest r) {
        return toResponse(universities.save(new University(userId, toData(r))), List.of());
    }

    @Transactional
    public UniversityResponse update(UUID userId, UUID id, UniversityRequest r) {
        University u = require(userId, id);
        u.update(toData(r));
        return toResponse(u, programs.findByUserIdAndUniversityIdOrderByNameAsc(userId, id));
    }

    /** Exclui a universidade com seus programas e candidaturas (cascata no banco). */
    @Transactional
    public void delete(UUID userId, UUID id) {
        universities.delete(require(userId, id));
    }

    private University.Data toData(UniversityRequest r) {
        Money cost = null;
        if (r.costOfLivingAmount() != null) {
            String currency = r.costOfLivingCurrency() != null ? r.costOfLivingCurrency() : "EUR";
            if (!currencies.isSupported(currency)) {
                throw new BusinessRuleException("Moeda não suportada.", "costOfLivingCurrency");
            }
            cost = Money.of(r.costOfLivingAmount(), currency);
        }
        return new University.Data(r.name(), r.ranking(), r.institutionType(), r.nature(), r.city(), r.region(),
                r.countryCode(), r.population(), r.latitude(), r.longitude(), cost, r.employability(), r.website(),
                r.notes());
    }

    static UniversityResponse toResponse(University u, List<Program> programs) {
        return new UniversityResponse(u.getId(), u.getName(), u.getRanking(), u.getInstitutionType(), u.getNature(),
                u.getCity(), u.getRegion(), u.getCountryCode(), u.getPopulation(), u.getLatitude(), u.getLongitude(),
                u.costOfLiving() == null ? null : MoneyResponse.of(u.costOfLiving()), u.getEmployability(),
                u.getWebsite(), u.getNotes(), u.getCoverImageId(), programs.size(),
                (int) programs.stream().filter(Program::isChosen).count());
    }

    static String norm(String s) {
        return s == null || s.isBlank() ? null : java.text.Normalizer.normalize(s.strip(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }
}
