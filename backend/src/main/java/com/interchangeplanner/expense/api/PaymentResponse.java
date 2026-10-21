package com.interchangeplanner.expense.api;

import com.interchangeplanner.expense.domain.ExpensePayment;
import com.interchangeplanner.shared.money.MoneyResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * @param amount                  valor pago, na moeda em que foi pago
 * @param amountInExpenseCurrency o mesmo valor na moeda do gasto, com a taxa capturada no pagamento
 */
public record PaymentResponse(
        UUID id,
        LocalDate paidOn,
        MoneyResponse amount,
        BigDecimal rateToExpenseCurrency,
        MoneyResponse amountInExpenseCurrency,
        @Schema(types = {"string", "null"}) String note) {

    public static PaymentResponse from(ExpensePayment p, String expenseCurrency) {
        return new PaymentResponse(p.getId(), p.getPaidOn(), MoneyResponse.of(p.money()), p.getRateToExpenseCurrency(),
                MoneyResponse.of(p.inExpenseCurrency(expenseCurrency)), p.getNote());
    }
}
