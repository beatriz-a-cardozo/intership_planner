package com.interchangeplanner.exchange.api;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record ExchangeRateOverrideRequest(
        @NotNull(message = "Informe a moeda base.") @Pattern(regexp = "[A-Z]{3}", message = "Moeda inválida.") String base,
        @NotNull(message = "Informe a moeda cotada.") @Pattern(regexp = "[A-Z]{3}", message = "Moeda inválida.") String quote,
        @NotNull(message = "Informe a cotação.")
        @Positive(message = "A cotação deve ser maior que zero.")
        @DecimalMax(value = "1000000", message = "Cotação fora do intervalo permitido.")
        @Digits(integer = 11, fraction = 8, message = "Use no máximo 8 casas decimais.")
        BigDecimal rate) {
}
