package com.interchangeplanner.exchange.domain;

import com.interchangeplanner.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Cotação definida manualmente pelo usuário. Quando ativa, prevalece sobre a de mercado para ele. */
@Entity
@Table(name = "user_exchange_rate_override")
public class UserExchangeRateOverride extends BaseEntity {

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "base", nullable = false, length = 3, updatable = false)
    private String base;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "quote", nullable = false, length = 3, updatable = false)
    private String quote;

    @Column(name = "rate", nullable = false, precision = 19, scale = 8)
    private BigDecimal rate;

    @Column(name = "active", nullable = false)
    private boolean active;

    protected UserExchangeRateOverride() {
    }

    public UserExchangeRateOverride(UUID userId, String base, String quote, BigDecimal rate) {
        this.userId = userId;
        this.base = base;
        this.quote = quote;
        this.rate = rate;
        this.active = true;
    }

    public void activate(BigDecimal rate) {
        this.rate = rate;
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
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

    public BigDecimal getRate() {
        return rate;
    }

    public boolean isActive() {
        return active;
    }
}
