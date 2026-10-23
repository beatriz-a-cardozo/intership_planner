package com.interchangeplanner.notification.domain;

import com.interchangeplanner.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Alerta de cotação ("avise quando o euro ficar abaixo de R$ 5,80"), com histerese (ver ExchangeAlertEvaluator). */
@Entity
@Table(name = "exchange_rate_alert")
public class ExchangeRateAlert extends BaseEntity {

    public enum Condition {
        BELOW,
        ABOVE
    }

    public enum State {
        ARMED,
        TRIGGERED
    }

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "base", nullable = false, length = 3)
    private String base;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "quote", nullable = false, length = 3)
    private String quote;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition", nullable = false, length = 10)
    private Condition condition;

    @Column(name = "threshold", nullable = false, precision = 19, scale = 8)
    private BigDecimal threshold;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 10)
    private State state = State.ARMED;

    @Column(name = "last_rate", precision = 19, scale = 8)
    private BigDecimal lastRate;

    @Column(name = "last_triggered_at")
    private Instant lastTriggeredAt;

    @Column(name = "trigger_count", nullable = false)
    private int triggerCount;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    protected ExchangeRateAlert() {
    }

    public ExchangeRateAlert(UUID userId, String base, String quote, Condition condition, BigDecimal threshold) {
        this.userId = userId;
        update(base, quote, condition, threshold, true);
    }

    /** Mudar a regra rearma o alerta. */
    public void update(String base, String quote, Condition condition, BigDecimal threshold, boolean active) {
        this.base = base;
        this.quote = quote;
        this.condition = condition;
        this.threshold = threshold;
        this.active = active;
        this.state = State.ARMED;
    }

    public void apply(State newState, BigDecimal rate, boolean triggered, Instant now) {
        this.state = newState;
        this.lastRate = rate;
        if (triggered) {
            this.lastTriggeredAt = now;
            this.triggerCount++;
        }
    }

    public UUID getUserId() {
        return userId;
    }

    public String getBase() {
        return base;
    }

    public String getQuote() {
        return quote;
    }

    public Condition getCondition() {
        return condition;
    }

    public BigDecimal getThreshold() {
        return threshold;
    }

    public State getState() {
        return state;
    }

    public BigDecimal getLastRate() {
        return lastRate;
    }

    public Instant getLastTriggeredAt() {
        return lastTriggeredAt;
    }

    public int getTriggerCount() {
        return triggerCount;
    }

    public boolean isActive() {
        return active;
    }
}
