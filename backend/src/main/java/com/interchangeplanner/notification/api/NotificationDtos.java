package com.interchangeplanner.notification.api;

import com.interchangeplanner.notification.domain.ExchangeRateAlert;
import com.interchangeplanner.notification.domain.Notification.Severity;
import com.interchangeplanner.notification.domain.NotificationCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class NotificationDtos {

    private NotificationDtos() {
    }

    public record NotificationResponse(
            UUID id,
            NotificationCategory category,
            Severity severity,
            String title,
            @Schema(types = {"string", "null"}) String body,
            @Schema(types = {"string", "null"}) String actionUrl,
            boolean read,
            Instant createdAt) {
    }

    /** Página de notificações (mais recentes primeiro). */
    public record NotificationPage(List<NotificationResponse> items, boolean hasMore, long unreadCount) {
    }

    public record UnreadCount(long count) {
    }

    /** Resultado de uma geração (quantas notificações novas). */
    public record RefreshResult(int created) {
    }

    /**
     * @param leadDays        antecedências em dias (ex.: [30, 7, 1])
     * @param defaultLeadDays as antecedências padrão da categoria
     * @param usesLeadDays    falso nas categorias sem data (câmbio, motivacional)
     */
    public record PreferenceResponse(NotificationCategory category, boolean enabled, List<Integer> leadDays,
            List<Integer> defaultLeadDays, boolean usesLeadDays) {
    }

    public record PreferenceRequest(
            @NotNull(message = "Informe se está ativa.") Boolean enabled,
            @Size(max = 5, message = "No máximo 5 antecedências.")
            List<@NotNull @Min(value = 0, message = "Mínimo 0 dias.") @Max(value = 365, message = "Máximo 365 dias.") Integer> leadDays) {
    }

    public record AlertRequest(
            @NotNull @Pattern(regexp = "[A-Z]{3}", message = "Moeda inválida.") String base,
            @NotNull @Pattern(regexp = "[A-Z]{3}", message = "Moeda inválida.") String quote,
            @NotNull(message = "Informe a condição.") ExchangeRateAlert.Condition condition,
            @NotNull(message = "Informe a cotação.") @Positive(message = "A cotação deve ser maior que zero.") BigDecimal threshold,
            Boolean active) {
    }

    /** @param state ARMED = vai avisar quando a condição valer; TRIGGERED = já avisou, rearma quando a cotação se afastar */
    public record AlertResponse(
            UUID id,
            String base,
            String quote,
            ExchangeRateAlert.Condition condition,
            BigDecimal threshold,
            ExchangeRateAlert.State state,
            @Schema(types = {"number", "null"}) BigDecimal lastRate,
            @Schema(types = {"string", "null"}, format = "date-time") Instant lastTriggeredAt,
            int triggerCount,
            boolean active) {
    }
}
