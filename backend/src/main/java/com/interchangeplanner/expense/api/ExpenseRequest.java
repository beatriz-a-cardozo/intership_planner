package com.interchangeplanner.expense.api;

import com.interchangeplanner.expense.domain.Recurrence;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * @param occurrences opcional: se ausente, o backend sugere a partir do horizonte do objetivo
 *                    (obrigatório para CUSTOM; sempre 1 para ONE_TIME e PER_UNIT)
 * @param quantity    padrão 1 (em PER_UNIT é o número de unidades, ex.: 5 inscrições)
 * @param required    padrão true
 */
public record ExpenseRequest(
        @NotNull(message = "Informe o objetivo.") UUID objectiveId,
        @NotNull(message = "Escolha a categoria.") UUID categoryId,
        UUID cycleId,
        @NotBlank(message = "Informe o nome do gasto.") @Size(max = 160, message = "Máximo de 160 caracteres.") String name,
        @Size(max = 2000, message = "Máximo de 2000 caracteres.") String description,
        @NotNull(message = "Informe a moeda.") @Pattern(regexp = "[A-Z]{3}", message = "Moeda inválida.") String currency,
        @NotNull(message = "Informe o preço unitário.")
        @PositiveOrZero(message = "O preço não pode ser negativo.")
        @Digits(integer = 15, fraction = 2, message = "Use no máximo 2 casas decimais.")
        BigDecimal unitPrice,
        @Min(value = 1, message = "A quantidade mínima é 1.") @Max(value = 10_000, message = "Quantidade muito alta.")
        Integer quantity,
        @NotNull(message = "Informe a periodicidade.") Recurrence recurrence,
        @Min(value = 1, message = "Mínimo de 1 ocorrência.") @Max(value = 1_200, message = "Ocorrências demais.")
        Integer occurrences,
        Boolean refundable,
        Boolean required,
        LocalDate dueDate,
        LocalDate endDate,
        @Size(max = 2000, message = "Máximo de 2000 caracteres.") String notes) {
}
