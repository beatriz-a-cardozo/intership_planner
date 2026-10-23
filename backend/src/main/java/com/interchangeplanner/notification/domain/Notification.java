package com.interchangeplanner.notification.domain;

import com.interchangeplanner.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Notificação in-app. É criada por insert direto com ON CONFLICT (deduplicação por {@code dedupeKey}); a entidade
 * serve para ler, marcar como lida e excluir.
 */
@Entity
@Table(name = "notification")
public class Notification extends BaseEntity {

    public enum Severity {
        INFO,
        WARNING,
        CRITICAL
    }

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, updatable = false, length = 20)
    private NotificationCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, updatable = false, length = 10)
    private Severity severity;

    @Column(name = "title", nullable = false, updatable = false, length = 200)
    private String title;

    @Column(name = "body", updatable = false, length = 1000)
    private String body;

    @Column(name = "action_url", updatable = false, length = 300)
    private String actionUrl;

    @Column(name = "source_type", updatable = false, length = 30)
    private String sourceType;

    @Column(name = "source_id", updatable = false)
    private UUID sourceId;

    @Column(name = "dedupe_key", nullable = false, updatable = false, length = 200)
    private String dedupeKey;

    @Column(name = "read_at")
    private Instant readAt;

    protected Notification() {
    }

    public void markRead(Instant now) {
        if (readAt == null) {
            readAt = now;
        }
    }

    public UUID getUserId() {
        return userId;
    }

    public NotificationCategory getCategory() {
        return category;
    }

    public Severity getSeverity() {
        return severity;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public String getActionUrl() {
        return actionUrl;
    }

    public String getSourceType() {
        return sourceType;
    }

    public UUID getSourceId() {
        return sourceId;
    }

    public String getDedupeKey() {
        return dedupeKey;
    }

    public Instant getReadAt() {
        return readAt;
    }
}
