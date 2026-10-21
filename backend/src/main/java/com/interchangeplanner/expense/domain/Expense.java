package com.interchangeplanner.expense.domain;

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

/** Gasto planejado. Guarda a moeda e o valor ORIGINAIS; equivalentes em outra moeda são calculados na leitura. */
@Entity
@Table(name = "expense")
public class Expense extends BaseEntity {

    /** Dados editáveis (criação e edição). {@code occurrences} já resolvido pela {@code ExpenseCalculator}. */
    public record Data(UUID objectiveId, UUID categoryId, UUID cycleId, String name, String description,
            String currency, BigDecimal unitPrice, int quantity, Recurrence recurrence, int occurrences,
            boolean refundable, boolean required, LocalDate dueDate, LocalDate endDate, String notes) {
    }

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "objective_id", nullable = false)
    private UUID objectiveId;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(name = "cycle_id")
    private UUID cycleId;

    @Column(name = "name", nullable = false, length = 160)
    private String name;

    @Column(name = "description", length = 2000)
    private String description;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitPrice;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "recurrence", nullable = false, length = 12)
    private Recurrence recurrence;

    @Column(name = "occurrences", nullable = false)
    private int occurrences;

    @Column(name = "refundable", nullable = false)
    private boolean refundable;

    @Column(name = "required", nullable = false)
    private boolean required;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "notes", length = 2000)
    private String notes;

    protected Expense() {
    }

    public static Expense create(UUID userId, Data data) {
        Expense e = new Expense();
        e.userId = userId;
        e.apply(data);
        return e;
    }

    public void update(Data data) {
        apply(data);
    }

    private void apply(Data d) {
        this.objectiveId = d.objectiveId();
        this.categoryId = d.categoryId();
        this.cycleId = d.cycleId();
        this.name = d.name().strip();
        this.description = blankToNull(d.description());
        this.currency = d.currency();
        this.unitPrice = d.unitPrice();
        this.quantity = d.quantity();
        this.recurrence = d.recurrence();
        this.occurrences = d.occurrences();
        this.refundable = d.refundable();
        this.required = d.required();
        this.dueDate = d.dueDate();
        this.endDate = d.endDate();
        this.notes = blankToNull(d.notes());
    }

    public void cancel(Instant when) {
        if (cancelledAt == null) {
            cancelledAt = when;
        }
    }

    public void restore() {
        cancelledAt = null;
    }

    public boolean isCancelled() {
        return cancelledAt != null;
    }

    public Money unitPriceMoney() {
        return Money.of(unitPrice, currency);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getObjectiveId() {
        return objectiveId;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public UUID getCycleId() {
        return cycleId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public Recurrence getRecurrence() {
        return recurrence;
    }

    public int getOccurrences() {
        return occurrences;
    }

    public boolean isRefundable() {
        return refundable;
    }

    public boolean isRequired() {
        return required;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public String getNotes() {
        return notes;
    }
}
