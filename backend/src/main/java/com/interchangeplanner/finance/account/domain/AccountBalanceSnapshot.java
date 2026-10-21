package com.interchangeplanner.finance.account.domain;

import com.interchangeplanner.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Saldo de uma conta numa data. Saldo atual = snapshot mais recente; o histórico alimenta o gráfico. */
@Entity
@Table(name = "account_balance_snapshot")
public class AccountBalanceSnapshot extends BaseEntity {

    @Column(name = "account_id", nullable = false, updatable = false)
    private UUID accountId;

    @Column(name = "as_of", nullable = false, updatable = false)
    private LocalDate asOf;

    @Column(name = "balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;

    protected AccountBalanceSnapshot() {
    }

    public AccountBalanceSnapshot(UUID accountId, LocalDate asOf, BigDecimal balance) {
        this.accountId = accountId;
        this.asOf = asOf;
        this.balance = balance;
    }

    public void changeBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public LocalDate getAsOf() {
        return asOf;
    }

    public BigDecimal getBalance() {
        return balance;
    }
}
