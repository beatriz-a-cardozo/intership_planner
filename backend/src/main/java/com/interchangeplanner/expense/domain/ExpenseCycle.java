package com.interchangeplanner.expense.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "expense_cycle")
public class ExpenseCycle extends CatalogItem {

    protected ExpenseCycle() {
    }

    public ExpenseCycle(UUID userId, String name, String color, String icon, int sortOrder) {
        super(userId, name, color, icon, sortOrder);
    }
}
