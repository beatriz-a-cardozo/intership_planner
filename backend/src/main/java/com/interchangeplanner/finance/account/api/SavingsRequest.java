package com.interchangeplanner.finance.account.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Guarda (valor positivo) ou retira (negativo) dinheiro para o objetivo: soma ao saldo de hoje de uma conta vinculada.
 *
 * @param accountId conta vinculada ao objetivo; vazio usa a primeira conta ativa na moeda ou cria "Reserva do
 *                  intercâmbio"
 * @param currency  moeda do valor quando não há conta escolhida (padrão: a do objetivo)
 */
public record SavingsRequest(
        @NotNull(message = "Informe o valor.") @Digits(integer = 15, fraction = 2, message = "Valor inválido.")
        BigDecimal amount,
        @Schema(types = {"string", "null"}) UUID accountId,
        @Schema(types = {"string", "null"}) @Pattern(regexp = "[A-Z]{3}", message = "Moeda inválida.") String currency) {
}
