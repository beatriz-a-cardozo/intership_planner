package com.interchangeplanner.expense.domain;

import com.interchangeplanner.shared.domain.BaseEntity;
import com.interchangeplanner.shared.money.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Pagamento (total ou parcial) de um gasto, em qualquer moeda. A taxa para a moeda do gasto é capturada
 * no momento do lançamento: o valor pago histórico não muda quando a cotação muda.
 */
@Entity
@Table(name = "expense_payment")
public class ExpensePayment extends BaseEntity {

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "expense_id", nullable = false, updatable = false)
    private UUID expenseId;

    @Column(name = "paid_on", nullable = false)
    private LocalDate paidOn;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "rate_to_expense_currency", nullable = false, precision = 19, scale = 8)
    private BigDecimal rateToExpenseCurrency;

    @Column(name = "note", length = 500)
    private String note;

    protected ExpensePayment() {
    }

    public ExpensePayment(UUID userId, UUID expenseId, LocalDate paidOn, Money amount, BigDecimal rateToExpenseCurrency,
            String note) {
        this.userId = userId;
        this.expenseId = expenseId;
        this.paidOn = paidOn;
        this.amount = amount.amount();
        this.currency = amount.currency();
        this.rateToExpenseCurrency = rateToExpenseCurrency;
        this.note = note == null || note.isBlank() ? null : note.strip();
    }

    public Money money() {
        return Money.of(amount, currency);
    }

    /** Valor na moeda do gasto, com a taxa do dia do pagamento. */
    public Money inExpenseCurrency(String expenseCurrency) {
        return Money.of(amount.multiply(rateToExpenseCurrency), expenseCurrency);
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getExpenseId() {
        return expenseId;
    }

    public LocalDate getPaidOn() {
        return paidOn;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getRateToExpenseCurrency() {
        return rateToExpenseCurrency;
    }

    public String getNote() {
        return note;
    }
}
