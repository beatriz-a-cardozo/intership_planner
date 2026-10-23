package com.interchangeplanner.university.api;

import com.interchangeplanner.applications.application.ApplicationService;
import com.interchangeplanner.applications.domain.ApplicationStatus;
import com.interchangeplanner.shared.security.CurrentUser;
import com.interchangeplanner.university.api.UniversityDtos.ChosenRequest;
import com.interchangeplanner.university.api.UniversityDtos.CompareResponse;
import com.interchangeplanner.university.api.UniversityDtos.ProgramRequest;
import com.interchangeplanner.university.api.UniversityDtos.ProgramResponse;
import com.interchangeplanner.university.api.UniversityDtos.UniversityDetailResponse;
import com.interchangeplanner.university.api.UniversityDtos.UniversityRequest;
import com.interchangeplanner.university.api.UniversityDtos.UniversityResponse;
import com.interchangeplanner.university.application.ProgramService;
import com.interchangeplanner.university.application.UniversityService;
import com.interchangeplanner.university.domain.UniversityEnums.InstitutionType;
import com.interchangeplanner.university.domain.UniversityEnums.Nature;
import com.interchangeplanner.university.domain.UniversityEnums.Rating;
import com.interchangeplanner.university.domain.UniversityEnums.Requirement;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "Universidades e programas")
public class UniversityController {

    private final UniversityService universities;
    private final ProgramService programs;
    private final ApplicationService applications;

    public UniversityController(UniversityService universities, ProgramService programs, ApplicationService applications) {
        this.universities = universities;
        this.programs = programs;
        this.applications = applications;
    }

    @GetMapping("/universities")
    @Operation(summary = "Lista universidades com filtros")
    public List<UniversityResponse> universities(CurrentUser user, @RequestParam(required = false) String country,
            @RequestParam(required = false) String city, @RequestParam(required = false) InstitutionType type,
            @RequestParam(required = false) Nature nature, @RequestParam(required = false) String q) {
        return universities.list(user.id(), new UniversityService.Filter(country, city, type, nature, q));
    }

    @GetMapping("/universities/{id}")
    @Operation(summary = "Universidade com seus programas")
    public UniversityDetailResponse university(CurrentUser user, @PathVariable UUID id) {
        return universities.get(user.id(), id);
    }

    @PostMapping("/universities")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria universidade")
    public UniversityResponse createUniversity(CurrentUser user, @Valid @RequestBody UniversityRequest r) {
        return universities.create(user.id(), r);
    }

    @PutMapping("/universities/{id}")
    @Operation(summary = "Atualiza universidade")
    public UniversityResponse updateUniversity(CurrentUser user, @PathVariable UUID id, @Valid @RequestBody UniversityRequest r) {
        return universities.update(user.id(), id, r);
    }

    @DeleteMapping("/universities/{id}")
    @Operation(summary = "Exclui universidade, programas e candidaturas")
    public ResponseEntity<Void> deleteUniversity(CurrentUser user, @PathVariable UUID id) {
        universities.delete(user.id(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/programs")
    @Operation(summary = "Lista programas com filtros (requisitos, chance, prazo, taxa em EUR, status da candidatura)")
    public List<ProgramResponse> programs(CurrentUser user,
            @RequestParam(required = false) UUID universityId,
            @RequestParam(required = false) Boolean chosen,
            @RequestParam(required = false) ApplicationStatus applicationStatus,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) Requirement gre,
            @RequestParam(required = false) Requirement interview,
            @RequestParam(required = false) Requirement nc,
            @RequestParam(required = false) Rating minAdmissionChance,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deadlineBefore,
            @RequestParam(required = false) BigDecimal maxSemesterFee,
            @RequestParam(required = false) String q) {
        return programs.list(user.id(), new ProgramService.Filter(universityId, chosen, applicationStatus, language, gre,
                interview, nc, minAdmissionChance, deadlineBefore, maxSemesterFee, q));
    }

    @GetMapping("/programs/compare")
    @Operation(summary = "Compara 2 a 5 programas lado a lado, com o melhor valor de cada linha")
    public CompareResponse compare(CurrentUser user, @RequestParam List<UUID> ids) {
        return programs.compare(user.id(), ids);
    }

    @GetMapping("/programs/{id}")
    @Operation(summary = "Detalhe do programa")
    public ProgramResponse program(CurrentUser user, @PathVariable UUID id) {
        return programs.get(user.id(), id);
    }

    @PostMapping("/programs")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria programa (com requisitos de idioma e intakes)")
    public ProgramResponse createProgram(CurrentUser user, @Valid @RequestBody ProgramRequest r) {
        return programs.create(user.id(), r);
    }

    @PutMapping("/programs/{id}")
    @Operation(summary = "Atualiza programa (requisitos e intakes são substituídos)")
    public ProgramResponse updateProgram(CurrentUser user, @PathVariable UUID id, @Valid @RequestBody ProgramRequest r) {
        return programs.update(user.id(), id, r);
    }

    @DeleteMapping("/programs/{id}")
    @Operation(summary = "Exclui programa e suas candidaturas")
    public ResponseEntity<Void> deleteProgram(CurrentUser user, @PathVariable UUID id) {
        programs.delete(user.id(), id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/programs/{id}/chosen")
    @Operation(summary = "Marca/desmarca como \"Escolhida\" (cria ou arquiva a candidatura)")
    public ProgramResponse choose(CurrentUser user, @PathVariable UUID id, @Valid @RequestBody ChosenRequest r) {
        return applications.choose(user.id(), id, r.chosen());
    }
}
