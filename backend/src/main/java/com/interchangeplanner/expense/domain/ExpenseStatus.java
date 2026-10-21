package com.interchangeplanner.expense.domain;

/** Derivado dos pagamentos (nunca gravado), exceto CANCELLED, que é uma ação manual. */
public enum ExpenseStatus {
    PENDING,
    PARTIALLY_PAID,
    PAID,
    CANCELLED
}
