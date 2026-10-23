package com.interchangeplanner.finance.ledger.domain;

import com.interchangeplanner.shared.domain.BaseEntity;
import com.interchangeplanner.shared.money.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Lançamento (receita ou despesa). Pode ser avulso ou gerado por um parcelamento/recorrência;
 * os gerados nascem PLANNED e são confirmados quando acontecem.
 */
@Entity
@Table(name = "finance_transaction")
public class FinanceTransaction extends BaseEntity {

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "account_id")
    private UUID accountId;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 10)
    private EntryKind kind;

    @Column(name = "occurred_on", nullable = false)
    private LocalDate occurredOn;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "description", nullable = false, length = 160)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private TransactionStatus status;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "installment_plan_id", updatable = false)
    private UUID installmentPlanId;

    @Column(name = "installment_number", updatable = false)
    private Short installmentNumber;

    @Column(name = "recurring_entry_id", updatable = false)
    private UUID recurringEntryId;

    protected FinanceTransaction() {
    }

    /** Lançamento avulso. */
    public static FinanceTransaction manual(UUID userId, EntryKind kind, UUID categoryId, UUID accountId,
            LocalDate occurredOn, Money amount, String description, TransactionStatus status, Instant now) {
        FinanceTransaction t = new FinanceTransaction();
        t.userId = userId;
        t.kind = kind;
        t.update(categoryId, accountId, occurredOn, amount, description);
        t.setStatus(status, now);
        return t;
    }

    public static FinanceTransaction installment(UUID userId, UUID planId, int number, UUID categoryId,
            UUID accountId, LocalDate dueDate, Money amount, String description) {
        FinanceTransaction t = manual(userId, EntryKind.EXPENSE, categoryId, accountId, dueDate, amount, description,
                TransactionStatus.PLANNED, null);
        t.installmentPlanId = planId;
        t.installmentNumber = (short) number;
        return t;
    }

    public static FinanceTransaction recurring(UUID userId, UUID entryId, EntryKind kind, UUID categoryId,
            UUID accountId, LocalDate date, Money amount, String description) {
        FinanceTransaction t = manual(userId, kind, categoryId, accountId, date, amount, description,
                TransactionStatus.PLANNED, null);
        t.recurringEntryId = entryId;
        return t;
    }

    public void update(UUID categoryId, UUID accountId, LocalDate occurredOn, Money amount, String description) {
        this.categoryId = categoryId;
        this.accountId = accountId;
        this.occurredOn = occurredOn;
        this.amount = amount.amount();
        this.currency = amount.currency();
        this.description = description.strip();
    }

    public void setStatus(TransactionStatus status, Instant now) {
        this.status = status;
        this.confirmedAt = status == TransactionStatus.CONFIRMED ? (confirmedAt != null ? confirmedAt : now) : null;
    }

    public boolean isConfirmed() {
        return status == TransactionStatus.CONFIRMED;
    }

    /** Gerado por parcelamento ou recorrência (não é avulso). */
    public boolean isScheduled() {
        return installmentPlanId != null || recurringEntryId != null;
    }

    public Money money() {
        return Money.of(amount, currency);
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public EntryKind getKind() {
        return kind;
    }

    public LocalDate getOccurredOn() {
        return occurredOn;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getDescription() {
        return description;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public Instant getConfirmedAt() {
        return confirmedAt;
    }

    public UUID getInstallmentPlanId() {
        return installmentPlanId;
    }

    public Integer getInstallmentNumber() {
        return installmentNumber == null ? null : installmentNumber.intValue();
    }

    public UUID getRecurringEntryId() {
        return recurringEntryId;
    }
}
