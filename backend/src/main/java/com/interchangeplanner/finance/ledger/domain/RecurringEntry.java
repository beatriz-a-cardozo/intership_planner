package com.interchangeplanner.finance.ledger.domain;

import com.interchangeplanner.finance.ledger.application.RecurrenceScheduler;
import com.interchangeplanner.shared.domain.BaseEntity;
import com.interchangeplanner.shared.money.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Assinatura, conta fixa ou receita recorrente (ex.: Spotify, aluguel, salário). */
@Entity
@Table(name = "recurring_entry")
public class RecurringEntry extends BaseEntity {

    public record Data(UUID categoryId, UUID accountId, String name, EntryKind kind, Money amount,
            Frequency frequency, Integer intervalMonths, Integer billingDay, LocalDate startDate, LocalDate endDate,
            boolean autoConfirm, String notes) {
    }

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(name = "account_id")
    private UUID accountId;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 10)
    private EntryKind kind;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", nullable = false, length = 16)
    private Frequency frequency;

    @Column(name = "interval_months")
    private Short intervalMonths;

    @Column(name = "billing_day")
    private Short billingDay;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "auto_confirm", nullable = false)
    private boolean autoConfirm;

    @Column(name = "notes", length = 1000)
    private String notes;

    protected RecurringEntry() {
    }

    public RecurringEntry(UUID userId, Data data) {
        this.userId = userId;
        this.active = true;
        update(data);
    }

    public void update(Data d) {
        this.categoryId = d.categoryId();
        this.accountId = d.accountId();
        this.name = d.name().strip();
        this.kind = d.kind();
        this.amount = d.amount().amount();
        this.currency = d.amount().currency();
        this.frequency = d.frequency();
        this.intervalMonths = d.frequency() == Frequency.EVERY_N_MONTHS && d.intervalMonths() != null
                ? d.intervalMonths().shortValue() : null;
        this.billingDay = d.frequency() == Frequency.WEEKLY || d.billingDay() == null ? null : d.billingDay().shortValue();
        this.startDate = d.startDate();
        this.endDate = d.endDate();
        this.autoConfirm = d.autoConfirm();
        this.notes = d.notes() == null || d.notes().isBlank() ? null : d.notes().strip();
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public RecurrenceScheduler.Rule rule() {
        return new RecurrenceScheduler.Rule(frequency, intervalMonths == null ? null : intervalMonths.intValue(),
                billingDay == null ? null : billingDay.intValue(), startDate, endDate);
    }

    public Money money() {
        return Money.of(amount, currency);
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public String getName() {
        return name;
    }

    public EntryKind getKind() {
        return kind;
    }

    public String getCurrency() {
        return currency;
    }

    public Frequency getFrequency() {
        return frequency;
    }

    public Integer getIntervalMonths() {
        return intervalMonths == null ? null : intervalMonths.intValue();
    }

    public Integer getBillingDay() {
        return billingDay == null ? null : billingDay.intValue();
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isAutoConfirm() {
        return autoConfirm;
    }

    public String getNotes() {
        return notes;
    }
}
