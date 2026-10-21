package com.interchangeplanner.finance.account.api;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Lança o saldo de uma conta numa data. Lançar de novo na mesma data substitui o valor.
 *
 * @param asOf data do saldo (padrão: hoje)
 */
public record BalanceRequest(
        LocalDate asOf,
        @NotNull(message = "Informe o saldo.") @Digits(integer = 15, fraction = 2, message = "Valor inválido.")
        BigDecimal balance) {
}
