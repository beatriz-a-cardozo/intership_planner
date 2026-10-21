package com.interchangeplanner.expense.api;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Pagamento em qualquer moeda.
 *
 * @param rateToExpenseCurrency taxa para a moeda do gasto; se ausente e as moedas diferirem, usa a cotação
 *                              vigente do usuário no momento do lançamento
 */
public record PaymentRequest(
        @NotNull(message = "Informe a data do pagamento.") LocalDate paidOn,
        @NotNull(message = "Informe o valor.")
        @Positive(message = "O valor deve ser maior que zero.")
        @Digits(integer = 15, fraction = 2, message = "Use no máximo 2 casas decimais.")
        BigDecimal amount,
        @NotNull(message = "Informe a moeda.") @Pattern(regexp = "[A-Z]{3}", message = "Moeda inválida.") String currency,
        @Positive(message = "A taxa deve ser maior que zero.") @Digits(integer = 11, fraction = 8) BigDecimal rateToExpenseCurrency,
        @Size(max = 500, message = "Máximo de 500 caracteres.") String note) {
}
