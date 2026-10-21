package com.interchangeplanner.onboarding;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Os 6 passos do onboarding: objetivo, país/cidade, data alvo, quanto já tem, economia mensal e moeda.
 *
 * @param targetAmount           opcional: informado = meta MANUAL; ausente = meta derivada dos gastos
 * @param currentSavings         opcional: cria a conta "Reserva do intercâmbio" com esse saldo
 * @param currentSavingsCurrency moeda do valor já guardado (padrão: moeda do objetivo)
 */
public record OnboardingRequest(
        @NotBlank(message = "Dê um nome ao seu objetivo.") @Size(max = 120, message = "Máximo de 120 caracteres.")
        String objectiveName,
        @NotNull(message = "Escolha o país.") @Pattern(regexp = "[A-Z]{2}", message = "País inválido.") String countryCode,
        @Size(max = 120, message = "Máximo de 120 caracteres.") String city,
        @NotNull(message = "Informe a data alvo.") LocalDate targetDate,
        @NotNull(message = "Escolha a moeda.") @Pattern(regexp = "[A-Z]{3}", message = "Moeda inválida.") String currency,
        @PositiveOrZero(message = "A meta não pode ser negativa.") @Digits(integer = 15, fraction = 2) BigDecimal targetAmount,
        @PositiveOrZero(message = "O valor não pode ser negativo.") @Digits(integer = 15, fraction = 2) BigDecimal currentSavings,
        @Pattern(regexp = "[A-Z]{3}", message = "Moeda inválida.") String currentSavingsCurrency,
        @PositiveOrZero(message = "O valor não pode ser negativo.") @Digits(integer = 15, fraction = 2)
        BigDecimal plannedMonthlySaving) {
}
