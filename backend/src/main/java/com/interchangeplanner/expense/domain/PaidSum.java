package com.interchangeplanner.expense.domain;

import java.math.BigDecimal;
import java.util.UUID;

/** Total pago de um gasto, já na moeda do gasto (Σ amount × taxa capturada). */
public record PaidSum(UUID expenseId, BigDecimal paid) {
}
