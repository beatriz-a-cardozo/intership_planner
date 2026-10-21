package com.interchangeplanner.expense.api;

import java.util.List;

public record ExpenseDetailResponse(ExpenseResponse expense, List<PaymentResponse> payments) {
}
