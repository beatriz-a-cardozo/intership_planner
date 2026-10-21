package com.interchangeplanner.expense.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "expense_category")
public class ExpenseCategory extends CatalogItem {

    protected ExpenseCategory() {
    }

    public ExpenseCategory(UUID userId, String name, String color, String icon, int sortOrder) {
        super(userId, name, color, icon, sortOrder);
    }
}
