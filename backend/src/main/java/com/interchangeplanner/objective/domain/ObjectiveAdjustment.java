package com.interchangeplanner.objective.domain;

import com.interchangeplanner.shared.domain.BaseEntity;
import com.interchangeplanner.shared.money.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Ajuste manual e visível da necessidade planejada (ex.: "Sperrkonto: −R$ 75.000"). Aplicado UMA vez,
 * independente do horizonte do objetivo — substitui o "−75000" escondido nas fórmulas da planilha.
 */
@Entity
@Table(name = "objective_adjustment")
public class ObjectiveAdjustment extends BaseEntity {

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "objective_id", nullable = false, updatable = false)
    private UUID objectiveId;

    @Column(name = "label", nullable = false, length = 120)
    private String label;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    protected ObjectiveAdjustment() {
    }

    public ObjectiveAdjustment(UUID userId, UUID objectiveId, String label, BigDecimal amount, String currency) {
        this.userId = userId;
        this.objectiveId = objectiveId;
        update(label, amount, currency);
    }

    public void update(String label, BigDecimal amount, String currency) {
        this.label = label.strip();
        this.amount = amount;
        this.currency = currency;
    }

    public Money money() {
        return Money.of(amount, currency);
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getObjectiveId() {
        return objectiveId;
    }

    public String getLabel() {
        return label;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }
}
