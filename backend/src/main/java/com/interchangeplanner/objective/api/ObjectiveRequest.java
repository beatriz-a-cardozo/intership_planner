package com.interchangeplanner.objective.api;

import com.interchangeplanner.objective.domain.ExpenseScope;
import com.interchangeplanner.objective.domain.ObjectiveStatus;
import com.interchangeplanner.objective.domain.TargetMode;
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

/** Campos opcionais com padrão: status ACTIVE, horizonte 24 meses, escopo REQUIRED_ONLY. */
public record ObjectiveRequest(
        @NotBlank(message = "Informe o nome do objetivo.") @Size(max = 120, message = "Máximo de 120 caracteres.")
        String name,
        @Size(max = 1000, message = "Máximo de 1000 caracteres.") String description,
        @Pattern(regexp = "[A-Z]{2}", message = "Use o código ISO do país (ex.: DE).") String countryCode,
        @Size(max = 120, message = "Máximo de 120 caracteres.") String city,
        LocalDate startDate,
        @NotNull(message = "Informe a data alvo.") LocalDate targetDate,
        @NotNull(message = "Informe a moeda.") @Pattern(regexp = "[A-Z]{3}", message = "Moeda inválida.") String currency,
        @NotNull(message = "Informe como a meta é definida.") TargetMode targetMode,
        @PositiveOrZero(message = "A meta não pode ser negativa.") @Digits(integer = 15, fraction = 2) BigDecimal targetAmount,
        @PositiveOrZero(message = "Valor inválido.") @Digits(integer = 15, fraction = 2) BigDecimal plannedMonthlySaving,
        ObjectiveStatus status,
        @Size(max = 20) String color,
        @Size(max = 40) String icon,
        @Min(value = 1, message = "Mínimo de 1 mês.") @Max(value = 120, message = "Máximo de 120 meses.") Integer horizonMonths,
        ExpenseScope expenseScope) {

    public ObjectiveRequest {
        countryCode = countryCode == null || countryCode.isBlank() ? null : countryCode.strip().toUpperCase();
    }
}
