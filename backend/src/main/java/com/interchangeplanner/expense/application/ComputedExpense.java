package com.interchangeplanner.expense.application;

import com.interchangeplanner.expense.domain.Expense;
import com.interchangeplanner.expense.domain.ExpenseStatus;
import com.interchangeplanner.shared.money.Money;

/** Gasto com os valores derivados (na moeda do próprio gasto). */
public record ComputedExpense(Expense expense, Money total, Money paid, Money remaining, ExpenseStatus status) {

    public boolean isCancelled() {
        return status == ExpenseStatus.CANCELLED;
    }
}
