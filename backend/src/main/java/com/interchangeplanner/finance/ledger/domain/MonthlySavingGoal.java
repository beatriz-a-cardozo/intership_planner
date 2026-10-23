package com.interchangeplanner.finance.ledger.domain;

import com.interchangeplanner.shared.domain.BaseEntity;
import com.interchangeplanner.shared.money.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Meta de economia de um mês (opcional). */
@Entity
@Table(name = "monthly_saving_goal")
public class MonthlySavingGoal extends BaseEntity {

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    /** Primeiro dia do mês. */
    @Column(name = "year_month", nullable = false, updatable = false)
    private LocalDate yearMonth;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    protected MonthlySavingGoal() {
    }

    public MonthlySavingGoal(UUID userId, YearMonth month, Money amount) {
        this.userId = userId;
        this.yearMonth = month.atDay(1);
        change(amount);
    }

    public void change(Money amount) {
        this.amount = amount.amount();
        this.currency = amount.currency();
    }

    public YearMonth month() {
        return YearMonth.from(yearMonth);
    }

    public Money money() {
        return Money.of(amount, currency);
    }

    public UUID getUserId() {
        return userId;
    }
}
