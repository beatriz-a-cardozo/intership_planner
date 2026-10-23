package com.interchangeplanner.university.api;

import com.interchangeplanner.applications.domain.ApplicationStatus;
import com.interchangeplanner.shared.money.MoneyResponse;
import com.interchangeplanner.university.application.IntakeWindowCalculator;
import com.interchangeplanner.university.domain.UniversityEnums.ApplicationChannel;
import com.interchangeplanner.university.domain.UniversityEnums.Cefr;
import com.interchangeplanner.university.domain.UniversityEnums.DocumentSubmission;
import com.interchangeplanner.university.domain.UniversityEnums.Employability;
import com.interchangeplanner.university.domain.UniversityEnums.InstitutionType;
import com.interchangeplanner.university.domain.UniversityEnums.Nature;
import com.interchangeplanner.university.domain.UniversityEnums.Rating;
import com.interchangeplanner.university.domain.UniversityEnums.Requirement;
import com.interchangeplanner.university.domain.UniversityEnums.Term;
import com.interchangeplanner.university.domain.UniversityEnums.TuitionPeriod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Contratos de universidades e programas (Fase 5). */
public final class UniversityDtos {

    private UniversityDtos() {
    }

    // ------------------------------------------------------------------ universidade

    public record UniversityRequest(
            @NotBlank(message = "Informe o nome.") @Size(max = 200) String name,
            @Min(value = 1, message = "Ranking deve ser positivo.") Integer ranking,
            InstitutionType institutionType,
            Nature nature,
            @Size(max = 120) String city,
            @Size(max = 120) String region,
            @Pattern(regexp = "[A-Z]{2}", message = "Use o código ISO do país (ex.: DE).") String countryCode,
            @PositiveOrZero Integer population,
            @DecimalMin("-90") @DecimalMax("90") @Digits(integer = 3, fraction = 6) BigDecimal latitude,
            @DecimalMin("-180") @DecimalMax("180") @Digits(integer = 3, fraction = 6) BigDecimal longitude,
            @PositiveOrZero @Digits(integer = 15, fraction = 2) BigDecimal costOfLivingAmount,
            @Pattern(regexp = "[A-Z]{3}", message = "Moeda inválida.") String costOfLivingCurrency,
            Employability employability,
            @Size(max = 500) String website,
            @Size(max = 2000) String notes) {
    }

    /**
     * @param programCount quantidade de programas cadastrados
     * @param chosenCount  programas marcados como "Escolhida"
     */
    public record UniversityResponse(
            UUID id,
            String name,
            @Schema(types = {"integer", "null"}) Integer ranking,
            @Schema(types = {"string", "null"}) InstitutionType institutionType,
            @Schema(types = {"string", "null"}) Nature nature,
            @Schema(types = {"string", "null"}) String city,
            @Schema(types = {"string", "null"}) String region,
            @Schema(types = {"string", "null"}) String countryCode,
            @Schema(types = {"integer", "null"}) Integer population,
            @Schema(types = {"number", "null"}) BigDecimal latitude,
            @Schema(types = {"number", "null"}) BigDecimal longitude,
            @Schema(types = {"object", "null"}) MoneyResponse costOfLiving,
            @Schema(types = {"string", "null"}) Employability employability,
            @Schema(types = {"string", "null"}) String website,
            @Schema(types = {"string", "null"}) String notes,
            @Schema(types = {"string", "null"}, format = "uuid") UUID coverImageId,
            int programCount,
            int chosenCount) {
    }

    /** Metadados de uma imagem da universidade; o conteúdo vem de {@code GET .../images/{id}/content}. */
    public record UniversityImageResponse(UUID id, String contentType, int sizeBytes,
            @Schema(types = {"string", "null"}) String caption, boolean cover, java.time.Instant createdAt) {
    }

    public record ImageCaptionRequest(@Size(max = 200, message = "Máximo de 200 caracteres.") String caption) {
    }

    public record UniversityDetailResponse(UniversityResponse university, List<ProgramResponse> programs) {
    }

    // ------------------------------------------------------------------ programa

    public record LanguageRequirementDto(
            @NotBlank(message = "Informe o idioma.") @Pattern(regexp = "[a-zA-Z]{2,5}", message = "Use o código do idioma (en, de).") String language,
            @Schema(types = {"string", "null"}) Cefr minCefr,
            @Schema(types = {"string", "null"}) @Size(max = 30) String testName,
            @Schema(types = {"number", "null"}) @PositiveOrZero @Digits(integer = 5, fraction = 1) BigDecimal minScore,
            @Schema(types = {"string", "null"}) @Size(max = 200) String rawText) {
    }

    public record IntakeDto(
            @NotNull(message = "Informe o semestre.") Term term,
            @Schema(types = {"integer", "null"}) @Min(1) @Max(12) Integer openMonth,
            @Schema(types = {"integer", "null"}) @Min(1) @Max(31) Integer openDay,
            @Schema(types = {"integer", "null"}) @Min(1) @Max(12) Integer closeMonth,
            @Schema(types = {"integer", "null"}) @Min(1) @Max(31) Integer closeDay,
            @NotNull(message = "Informe o mês de início.") @Min(1) @Max(12) Integer startMonth) {
    }

    public record ProgramRequest(
            @NotNull(message = "Escolha a universidade.") UUID universityId,
            @NotBlank(message = "Informe o nome do programa.") @Size(max = 200) String name,
            @Size(max = 500) String area,
            @Size(max = 2000) String description,
            @Size(max = 500) String officialUrl,
            @Size(max = 40) String degree,
            @Size(max = 60) String languageSummary,
            @Min(1) @Max(20) Integer durationSemesters,
            Requirement gre,
            Requirement aptitudeAssessment,
            Requirement interview,
            Requirement nc,
            ApplicationChannel applicationChannel,
            @Size(max = 60) String portalName,
            DocumentSubmission documentSubmission,
            @PositiveOrZero @Digits(integer = 15, fraction = 2) BigDecimal semesterFeeAmount,
            @Pattern(regexp = "[A-Z]{3}", message = "Moeda inválida.") String semesterFeeCurrency,
            @PositiveOrZero @Digits(integer = 15, fraction = 2) BigDecimal nonEuTuitionAmount,
            @Pattern(regexp = "[A-Z]{3}", message = "Moeda inválida.") String nonEuTuitionCurrency,
            TuitionPeriod nonEuTuitionPeriod,
            Boolean scholarshipAvailable,
            @Size(max = 500) String scholarshipDetails,
            Rating scholarshipChance,
            Rating admissionChance,
            @Size(max = 200) String applicationDeadlineText,
            @Size(max = 2000) String notes,
            @Valid @Size(max = 10) List<LanguageRequirementDto> languageRequirements,
            @Valid @Size(max = 4) List<IntakeDto> intakes) {
    }

    public record IntakeResponse(Term term, @Schema(types = {"integer", "null"}) Integer openMonth,
            @Schema(types = {"integer", "null"}) Integer openDay, @Schema(types = {"integer", "null"}) Integer closeMonth,
            @Schema(types = {"integer", "null"}) Integer closeDay, int startMonth, NextWindow nextWindow) {
    }

    /** Próximo ciclo concreto calculado no backend (ver IntakeWindowCalculator). */
    public record NextWindow(int intakeYear, @Schema(types = {"string", "null"}, format = "date") LocalDate opensOn,
            @Schema(types = {"string", "null"}, format = "date") LocalDate closesOn, LocalDate startsOn,
            IntakeWindowCalculator.State state) {
    }

    public record UniversitySummary(UUID id, String name, @Schema(types = {"string", "null"}) String city,
            @Schema(types = {"string", "null"}) String countryCode, @Schema(types = {"integer", "null"}) Integer ranking,
            @Schema(types = {"string", "null"}) InstitutionType institutionType,
            @Schema(types = {"string", "null"}) Nature nature,
            @Schema(types = {"object", "null"}) MoneyResponse costOfLiving,
            @Schema(types = {"string", "null"}) Employability employability,
            @Schema(types = {"string", "null"}, format = "uuid") UUID coverImageId) {
    }

    /** Candidatura ativa do programa (para o selo e o status direto no card). */
    public record ApplicationSummary(UUID id, ApplicationStatus status, ApplicationStatus.Result result, int intakeYear,
            Term term) {
    }

    /**
     * @param nextDeadline prazo mais próximo entre os intakes (nulo sem prazo conhecido)
     */
    public record ProgramResponse(
            UUID id,
            UniversitySummary university,
            String name,
            @Schema(types = {"string", "null"}) String area,
            @Schema(types = {"string", "null"}) String description,
            @Schema(types = {"string", "null"}) String officialUrl,
            @Schema(types = {"string", "null"}) String degree,
            @Schema(types = {"string", "null"}) String languageSummary,
            @Schema(types = {"integer", "null"}) Integer durationSemesters,
            Requirement gre,
            Requirement aptitudeAssessment,
            Requirement interview,
            Requirement nc,
            @Schema(types = {"string", "null"}) ApplicationChannel applicationChannel,
            @Schema(types = {"string", "null"}) String portalName,
            @Schema(types = {"string", "null"}) DocumentSubmission documentSubmission,
            @Schema(types = {"object", "null"}) MoneyResponse semesterFee,
            @Schema(types = {"object", "null"}) MoneyResponse nonEuTuition,
            @Schema(types = {"string", "null"}) TuitionPeriod nonEuTuitionPeriod,
            @Schema(types = {"boolean", "null"}) Boolean scholarshipAvailable,
            @Schema(types = {"string", "null"}) String scholarshipDetails,
            @Schema(types = {"string", "null"}) Rating scholarshipChance,
            @Schema(types = {"string", "null"}) Rating admissionChance,
            @Schema(types = {"string", "null"}) String applicationDeadlineText,
            boolean chosen,
            @Schema(types = {"string", "null"}) String notes,
            List<LanguageRequirementDto> languageRequirements,
            List<IntakeResponse> intakes,
            @Schema(types = {"string", "null"}, format = "date") LocalDate nextDeadline,
            @Schema(types = {"object", "null"}) ApplicationSummary application) {
    }

    public record ChosenRequest(@NotNull Boolean chosen) {
    }

    /**
     * @param highlights para cada linha comparável, os ids dos programas com o melhor valor
     *                   (ranking, semesterFee, nonEuTuition, costOfLiving, admissionChance, scholarshipChance,
     *                   employability, nextDeadline)
     */
    public record CompareResponse(List<ProgramResponse> programs, Map<String, List<UUID>> highlights) {
    }
}
