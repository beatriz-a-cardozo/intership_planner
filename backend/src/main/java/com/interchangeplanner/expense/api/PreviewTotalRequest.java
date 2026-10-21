package com.interchangeplanner.expense.api;

import com.interchangeplanner.expense.domain.Recurrence;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.UUID;

/** Mesmo cálculo do salvamento, sem gravar: o formulário mostra o total em tempo real sem duplicar a regra. */
public record PreviewTotalRequest(
        @NotNull(message = "Informe o objetivo.") UUID objectiveId,
        @NotNull(message = "Informe a moeda.") @Pattern(regexp = "[A-Z]{3}", message = "Moeda inválida.") String currency,
        @NotNull(message = "Informe o preço unitário.") @PositiveOrZero(message = "O preço não pode ser negativo.")
        BigDecimal unitPrice,
        @Min(value = 1, message = "A quantidade mínima é 1.") @Max(10_000) Integer quantity,
        @NotNull(message = "Informe a periodicidade.") Recurrence recurrence,
        @Min(value = 1, message = "Mínimo de 1 ocorrência.") @Max(1_200) Integer occurrences) {
}
