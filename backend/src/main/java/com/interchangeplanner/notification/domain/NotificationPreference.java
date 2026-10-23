package com.interchangeplanner.notification.domain;

import com.interchangeplanner.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Preferência por categoria. Sem linha gravada vale o padrão da categoria (ativa, antecedências padrão). */
@Entity
@Table(name = "notification_preference")
public class NotificationPreference extends BaseEntity {

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, updatable = false, length = 20)
    private NotificationCategory category;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "lead_days", nullable = false)
    private Integer[] leadDays;

    protected NotificationPreference() {
    }

    public NotificationPreference(UUID userId, NotificationCategory category, boolean enabled, List<Integer> leadDays) {
        this.userId = userId;
        this.category = category;
        update(enabled, leadDays);
    }

    public void update(boolean enabled, List<Integer> leadDays) {
        this.enabled = enabled;
        this.leadDays = leadDays.stream().distinct().sorted((a, b) -> b - a).toArray(Integer[]::new);
    }

    public UUID getUserId() {
        return userId;
    }

    public NotificationCategory getCategory() {
        return category;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public List<Integer> getLeadDays() {
        return leadDays == null ? List.of() : Arrays.asList(leadDays);
    }
}
