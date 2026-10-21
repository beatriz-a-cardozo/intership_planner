package com.interchangeplanner.finance.account.domain;

import com.interchangeplanner.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Conta onde o dinheiro está guardado. Cada conta tem UMA moeda; BRL e EUR nunca se misturam. */
@Entity
@Table(name = "account")
public class Account extends BaseEntity {

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "objective_id")
    private UUID objectiveId;

    @Column(name = "name", nullable = false, length = 80)
    private String name;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "currency", nullable = false, length = 3, updatable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 15)
    private AccountType type;

    @Column(name = "archived", nullable = false)
    private boolean archived;

    protected Account() {
    }

    public Account(UUID userId, String name, String currency, AccountType type, UUID objectiveId) {
        this.userId = userId;
        this.currency = currency;
        update(name, type, objectiveId, false);
    }

    /** A moeda não muda depois de criada: o histórico de saldos está nela. */
    public void update(String name, AccountType type, UUID objectiveId, boolean archived) {
        this.name = name.strip();
        this.type = type;
        this.objectiveId = objectiveId;
        this.archived = archived;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getObjectiveId() {
        return objectiveId;
    }

    public String getName() {
        return name;
    }

    public String getCurrency() {
        return currency;
    }

    public AccountType getType() {
        return type;
    }

    public boolean isArchived() {
        return archived;
    }
}
