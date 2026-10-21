package com.interchangeplanner.objective.api;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** @param amount com sinal: negativo reduz a necessidade planejada (ex.: −75000 do Sperrkonto) */
public record AdjustmentRequest(
        @NotBlank(message = "Informe a descrição do ajuste.") @Size(max = 120, message = "Máximo de 120 caracteres.")
        String label,
        @NotNull(message = "Informe o valor.") @Digits(integer = 15, fraction = 2, message = "Valor inválido.")
        BigDecimal amount,
        @NotNull(message = "Informe a moeda.") @Pattern(regexp = "[A-Z]{3}", message = "Moeda inválida.")
        String currency) {
}
