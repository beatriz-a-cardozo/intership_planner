package com.interchangeplanner.task.api;

import com.interchangeplanner.shared.domain.Priority;
import com.interchangeplanner.task.domain.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class TaskDtos {

    private TaskDtos() {
    }

    public record TaskRequest(
            @NotBlank(message = "Informe a tarefa.") @Size(max = 200, message = "Máximo de 200 caracteres.") String title,
            @Size(max = 2000, message = "Máximo de 2000 caracteres.") String description,
            Priority priority,
            TaskStatus status,
            LocalDate dueDate,
            @Size(max = 40, message = "Máximo de 40 caracteres.") String category,
            UUID objectiveId,
            UUID universityId,
            UUID expenseId,
            UUID applicationId) {
    }

    public record TaskStatusRequest(@NotNull(message = "Informe o status.") TaskStatus status) {
    }

    /**
     * Vínculo exibido na tarefa.
     *
     * @param parentId para candidaturas, o programa (a tela da candidatura fica no programa)
     */
    public record LinkRef(UUID id, String label, @Schema(types = {"string", "null"}, format = "uuid") UUID parentId) {
    }

    /** @param overdue prazo passou e a tarefa não foi concluída */
    public record TaskResponse(
            UUID id,
            String title,
            @Schema(types = {"string", "null"}) String description,
            Priority priority,
            TaskStatus status,
            @Schema(types = {"string", "null"}, format = "date") LocalDate dueDate,
            @Schema(types = {"string", "null"}) String category,
            boolean overdue,
            @Schema(types = {"string", "null"}, format = "date-time") Instant completedAt,
            @Schema(types = {"object", "null"}) LinkRef objective,
            @Schema(types = {"object", "null"}) LinkRef university,
            @Schema(types = {"object", "null"}) LinkRef expense,
            @Schema(types = {"object", "null"}) LinkRef application,
            Instant createdAt) {
    }
}
