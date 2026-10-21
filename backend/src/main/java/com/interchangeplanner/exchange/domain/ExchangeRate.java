package com.interchangeplanner.exchange.domain;

import com.interchangeplanner.shared.domain.UuidV7;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Cotação obtida de um provedor. Registro imutável (histórico). */
@Entity
@Immutable
@Table(name = "exchange_rate")
public class ExchangeRate {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id = UuidV7.generate();

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "base", nullable = false, length = 3)
    private String base;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "quote", nullable = false, length = 3)
    private String quote;

    @Column(name = "rate", nullable = false, precision = 19, scale = 8)
    private BigDecimal rate;

    @Column(name = "source", nullable = false, length = 30)
    private String source;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt;

    protected ExchangeRate() {
    }

    public ExchangeRate(String base, String quote, BigDecimal rate, String source, LocalDate effectiveDate,
            Instant fetchedAt) {
        this.base = base;
        this.quote = quote;
        this.rate = rate;
        this.source = source;
        this.effectiveDate = effectiveDate;
        this.fetchedAt = fetchedAt;
    }

    public UUID getId() {
        return id;
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

    public String getSource() {
        return source;
    }

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public Instant getFetchedAt() {
        return fetchedAt;
    }
}
