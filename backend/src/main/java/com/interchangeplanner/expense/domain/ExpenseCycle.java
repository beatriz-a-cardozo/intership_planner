package com.interchangeplanner.expense.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "expense_cycle")
public class ExpenseCycle extends CatalogItem {

    /** Os gastos deste ciclo entram no valor necessário da meta (padrão: Preparação e Imigração). */
    @Column(name = "in_goal", nullable = false)
    private boolean inGoal;

    protected ExpenseCycle() {
    }

    public ExpenseCycle(UUID userId, String name, String color, String icon, int sortOrder) {
        super(userId, name, color, icon, sortOrder);
    }

    public boolean isInGoal() {
        return inGoal;
    }

    public void setInGoal(boolean inGoal) {
        this.inGoal = inGoal;
    }
}
