package com.interchangeplanner.notification.api;

import com.interchangeplanner.notification.api.NotificationDtos.AlertRequest;
import com.interchangeplanner.notification.api.NotificationDtos.AlertResponse;
import com.interchangeplanner.notification.api.NotificationDtos.NotificationPage;
import com.interchangeplanner.notification.api.NotificationDtos.NotificationResponse;
import com.interchangeplanner.notification.api.NotificationDtos.PreferenceRequest;
import com.interchangeplanner.notification.api.NotificationDtos.PreferenceResponse;
import com.interchangeplanner.notification.api.NotificationDtos.RefreshResult;
import com.interchangeplanner.notification.api.NotificationDtos.UnreadCount;
import com.interchangeplanner.notification.application.ExchangeAlertService;
import com.interchangeplanner.notification.application.NotificationPreferenceService;
import com.interchangeplanner.notification.application.NotificationService;
import com.interchangeplanner.notification.domain.NotificationCategory;
import com.interchangeplanner.shared.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
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
@Tag(name = "Notificações")
public class NotificationController {

    private final NotificationService notifications;
    private final NotificationPreferenceService preferences;
    private final ExchangeAlertService alerts;

    public NotificationController(NotificationService notifications, NotificationPreferenceService preferences,
            ExchangeAlertService alerts) {
        this.notifications = notifications;
        this.preferences = preferences;
        this.alerts = alerts;
    }

    @GetMapping("/notifications")
    @Operation(summary = "Notificações, mais recentes primeiro (unread=true só as não lidas)")
    public NotificationPage list(CurrentUser user, @RequestParam(defaultValue = "false") boolean unread,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return notifications.list(user.id(), unread, page, size);
    }

    @GetMapping("/notifications/unread-count")
    @Operation(summary = "Quantidade de não lidas (badge do sino)")
    public UnreadCount unreadCount(CurrentUser user) {
        return new UnreadCount(notifications.unreadCount(user.id()));
    }

    @PostMapping("/notifications/refresh")
    @Operation(summary = "Gera agora as notificações da pessoa (idempotente: não repete avisos)")
    public RefreshResult refresh(CurrentUser user) {
        return new RefreshResult(notifications.generateFor(user.id()));
    }

    @PatchMapping("/notifications/{id}/read")
    @Operation(summary = "Marca como lida")
    public NotificationResponse read(CurrentUser user, @PathVariable UUID id) {
        return notifications.markRead(user.id(), id);
    }

    @PostMapping("/notifications/read-all")
    @Operation(summary = "Marca todas como lidas")
    public UnreadCount readAll(CurrentUser user) {
        notifications.markAllRead(user.id());
        return new UnreadCount(0);
    }

    @DeleteMapping("/notifications/{id}")
    @Operation(summary = "Exclui a notificação")
    public ResponseEntity<Void> delete(CurrentUser user, @PathVariable UUID id) {
        notifications.delete(user.id(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/notification-preferences")
    @Operation(summary = "Preferências por categoria (padrões quando nunca alteradas)")
    public List<PreferenceResponse> preferences(CurrentUser user) {
        return preferences.list(user.id());
    }

    @PutMapping("/notification-preferences/{category}")
    @Operation(summary = "Ativa/desativa a categoria e define as antecedências")
    public PreferenceResponse updatePreference(CurrentUser user, @PathVariable NotificationCategory category,
            @Valid @RequestBody PreferenceRequest r) {
        return preferences.update(user.id(), category, r);
    }

    @GetMapping("/exchange-rate-alerts")
    @Operation(summary = "Alertas de câmbio")
    public List<AlertResponse> alerts(CurrentUser user) {
        return alerts.list(user.id());
    }

    @PostMapping("/exchange-rate-alerts")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria alerta de câmbio (avaliado na hora e a cada atualização de cotação)")
    public AlertResponse createAlert(CurrentUser user, @Valid @RequestBody AlertRequest r) {
        return alerts.create(user.id(), r);
    }

    @PutMapping("/exchange-rate-alerts/{id}")
    @Operation(summary = "Edita alerta (rearma)")
    public AlertResponse updateAlert(CurrentUser user, @PathVariable UUID id, @Valid @RequestBody AlertRequest r) {
        return alerts.update(user.id(), id, r);
    }

    @DeleteMapping("/exchange-rate-alerts/{id}")
    @Operation(summary = "Exclui alerta")
    public ResponseEntity<Void> deleteAlert(CurrentUser user, @PathVariable UUID id) {
        alerts.delete(user.id(), id);
        return ResponseEntity.noContent().build();
    }
}
