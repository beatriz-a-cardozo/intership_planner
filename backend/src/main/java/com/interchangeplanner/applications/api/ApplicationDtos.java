package com.interchangeplanner.applications.api;

import com.interchangeplanner.applications.domain.Application.Confidence;
import com.interchangeplanner.applications.domain.ApplicationRequest.Type;
import com.interchangeplanner.applications.domain.ApplicationStatus;
import com.interchangeplanner.university.application.IntakeWindowCalculator;
import com.interchangeplanner.university.domain.UniversityEnums.Term;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

/** Contratos de candidaturas (Fase 5). */
public final class ApplicationDtos {

    private ApplicationDtos() {
    }

    /**
     * Cria uma candidatura. Sem ano/semestre, usa o próximo ciclo do primeiro intake do programa; sem prazo,
     * usa o fechamento da janela desse ciclo. Sem objetivo, usa o principal.
     */
    public record ApplicationCreateRequest(
            @NotNull(message = "Escolha o programa.") UUID programId,
            UUID objectiveId,
            @Min(2000) @Max(2100) Integer intakeYear,
            Term term,
            LocalDate deadline,
            @Size(max = 2000) String notes) {
    }

    /** Dados editáveis (o status muda por {@code PATCH /status}, que registra histórico). */
    public record ApplicationUpdateRequest(
            UUID objectiveId,
            @NotNull(message = "Informe o ano do intake.") @Min(2000) @Max(2100) Integer intakeYear,
            @NotNull(message = "Informe o semestre.") Term term,
            LocalDate submittedAt,
            LocalDate deadline,
            LocalDate responseMinDate,
            LocalDate responseLikelyDate,
            LocalDate responseMaxDate,
            Confidence responseConfidence,
            @Size(max = 500) String responseEstimateBasis,
            LocalDate actualResponseDate,
            @Size(max = 2000) String notes) {
    }

    /** @param changedOn data da mudança (padrão: hoje) */
    public record StatusChangeRequest(
            @NotNull(message = "Informe o status.") ApplicationStatus status,
            LocalDate changedOn,
            @Size(max = 500) String note) {
    }

    public record ProgramRef(UUID id, String name, UUID universityId, String universityName,
            @Schema(types = {"string", "null"}) String city, @Schema(types = {"string", "null"}) String countryCode) {
    }

    public record CycleWindow(@Schema(types = {"string", "null"}, format = "date") LocalDate opensOn,
            @Schema(types = {"string", "null"}, format = "date") LocalDate closesOn, LocalDate startsOn,
            IntakeWindowCalculator.State state) {
    }

    /**
     * @param result          derivado do status
     * @param window          janela de inscrição do ciclo (nula se o programa não tem intake desse semestre)
     * @param pendingRequests solicitações da universidade ainda não atendidas
     */
    public record ApplicationResponse(
            UUID id,
            ProgramRef program,
            @Schema(types = {"string", "null"}, format = "uuid") UUID objectiveId,
            ApplicationStatus status,
            ApplicationStatus.Result result,
            int intakeYear,
            Term term,
            boolean archived,
            @Schema(types = {"string", "null"}, format = "date") LocalDate submittedAt,
            @Schema(types = {"string", "null"}, format = "date") LocalDate deadline,
            @Schema(types = {"string", "null"}, format = "date") LocalDate responseMinDate,
            @Schema(types = {"string", "null"}, format = "date") LocalDate responseLikelyDate,
            @Schema(types = {"string", "null"}, format = "date") LocalDate responseMaxDate,
            @Schema(types = {"string", "null"}) Confidence responseConfidence,
            @Schema(types = {"string", "null"}) String responseEstimateBasis,
            @Schema(types = {"string", "null"}, format = "date") LocalDate actualResponseDate,
            @Schema(types = {"string", "null"}) String notes,
            @Schema(types = {"object", "null"}) CycleWindow window,
            int pendingRequests,
            @Schema(types = {"string", "null"}, format = "date") LocalDate nextRequestDue) {
    }

    public record StatusHistoryResponse(UUID id, ApplicationStatus status, LocalDate changedOn,
            @Schema(types = {"string", "null"}) String note) {
    }

    /** @param requestedOn padrão: hoje */
    public record RequestRequest(
            @NotNull(message = "Informe o tipo.") Type type,
            @NotBlank(message = "Descreva a solicitação.") @Size(max = 500) String description,
            LocalDate requestedOn,
            LocalDate dueDate,
            LocalDate fulfilledOn) {
    }

    public record RequestResponse(UUID id, Type type, String description, LocalDate requestedOn,
            @Schema(types = {"string", "null"}, format = "date") LocalDate dueDate,
            @Schema(types = {"string", "null"}, format = "date") LocalDate fulfilledOn, boolean pending,
            boolean overdue) {
    }
}
