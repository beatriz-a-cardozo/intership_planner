package com.interchangeplanner.timeline.api;

import com.interchangeplanner.shared.domain.Priority;
import com.interchangeplanner.shared.money.MoneyResponse;
import com.interchangeplanner.timeline.domain.EventType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class TimelineDtos {

    private TimelineDtos() {
    }

    /**
     * De onde o item vem. Só EVENT é gravado; os demais são projetados das entidades de origem. A timeline é do
     * planejamento: gastos com data, períodos das universidades (candidaturas e solicitações) e tarefas. Finanças
     * (parcelas, assinaturas, lançamentos) ficam fora.
     */
    public enum Source {
        EVENT,
        EXPENSE,
        APPLICATION,
        REQUEST,
        TASK
    }

    /** O que o item representa (define ícone e cor). */
    public enum Kind {
        DEADLINE,
        PAYMENT_DUE,
        WINDOW_OPENS,
        RESPONSE,
        TASK,
        MILESTONE,
        EVENT
    }

    /** DONE = aconteceu/foi feito; OVERDUE = passou da data sem ser feito; PENDING = ainda vai acontecer. */
    public enum ItemStatus {
        DONE,
        PENDING,
        OVERDUE
    }

    /**
     * Item da timeline.
     *
     * @param id       determinístico ({@code expense:{uuid}:due:0}, {@code application:{uuid}:deadline}...): estável
     *                 entre chamadas, mesmo sem tabela
     * @param sourceId entidade de origem (gasto, parcelamento, candidatura, tarefa, evento...)
     * @param parentId entidade "mãe" para navegação (programa da candidatura, gasto do pagamento)
     * @param endDate  fim de um período (evento de vários dias, faixa da resposta esperada)
     */
    public record TimelineItem(
            String id,
            Source source,
            Kind kind,
            LocalDate date,
            @Schema(types = {"string", "null"}, format = "date") LocalDate endDate,
            String title,
            @Schema(types = {"string", "null"}) String subtitle,
            ItemStatus status,
            @Schema(types = {"string", "null"}) Priority priority,
            @Schema(types = {"object", "null"}) MoneyResponse amount,
            UUID sourceId,
            @Schema(types = {"string", "null"}, format = "uuid") UUID parentId,
            @Schema(types = {"string", "null"}) EventType eventType,
            @Schema(types = {"string", "null"}) String description) {
    }

    public record TimelineResponse(LocalDate from, LocalDate to, LocalDate today, List<TimelineItem> items) {
    }

    public record EventRequest(
            @NotBlank(message = "Informe o título.") @Size(max = 200, message = "Máximo de 200 caracteres.") String title,
            @Size(max = 2000, message = "Máximo de 2000 caracteres.") String description,
            @NotNull(message = "Informe a data.") LocalDate startDate,
            LocalDate endDate,
            EventType type,
            Priority priority,
            Boolean done,
            UUID objectiveId) {
    }

    public record EventDoneRequest(@NotNull Boolean done) {
    }
}
