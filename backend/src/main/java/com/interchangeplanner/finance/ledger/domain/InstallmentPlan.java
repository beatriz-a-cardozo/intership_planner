package com.interchangeplanner.finance.ledger.domain;

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

/**
 * Compra parcelada. As parcelas são lançamentos PLANNED vinculados (ver InstallmentCalculator).
 * {@code paidBeforeCount} parcelas foram pagas antes de entrar no app (não viram lançamentos) e
 * {@code firstDueDate} é o vencimento da primeira parcela acompanhada aqui (a nº paidBeforeCount + 1).
 */
@Entity
@Table(name = "installment_plan")
public class InstallmentPlan extends BaseEntity {

    public record Data(UUID categoryId, UUID accountId, String description, Money total, int installmentsCount,
            int paidBeforeCount, LocalDate firstDueDate, PaymentMethod paymentMethod, String notes) {
    }

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(name = "account_id")
    private UUID accountId;

    @Column(name = "description", nullable = false, length = 160)
    private String description;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalAmount;

    @Column(name = "installments_count", nullable = false)
    private short installmentsCount;

    @Column(name = "paid_before_count", nullable = false)
    private short paidBeforeCount;

    @Column(name = "first_due_date", nullable = false)
    private LocalDate firstDueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Column(name = "notes", length = 1000)
    private String notes;

    protected InstallmentPlan() {
    }

    public InstallmentPlan(UUID userId, Data data) {
        this.userId = userId;
        update(data);
    }

    public void update(Data d) {
        this.categoryId = d.categoryId();
        this.accountId = d.accountId();
        this.description = d.description().strip();
        this.currency = d.total().currency();
        this.totalAmount = d.total().amount();
        this.installmentsCount = (short) d.installmentsCount();
        this.paidBeforeCount = (short) d.paidBeforeCount();
        this.firstDueDate = d.firstDueDate();
        this.paymentMethod = d.paymentMethod();
        this.notes = d.notes() == null || d.notes().isBlank() ? null : d.notes().strip();
    }

    public Money total() {
        return Money.of(totalAmount, currency);
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

    public String getDescription() {
        return description;
    }

    public String getCurrency() {
        return currency;
    }

    public int getInstallmentsCount() {
        return installmentsCount;
    }

    public int getPaidBeforeCount() {
        return paidBeforeCount;
    }

    public LocalDate getFirstDueDate() {
        return firstDueDate;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public String getNotes() {
        return notes;
    }
}
