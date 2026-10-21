package com.interchangeplanner.objective.api;

import com.interchangeplanner.objective.domain.ExpenseScope;
import com.interchangeplanner.objective.domain.Objective;
import com.interchangeplanner.objective.domain.ObjectiveStatus;
import com.interchangeplanner.objective.domain.TargetMode;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.UUID;

public record ObjectiveResponse(
        UUID id,
        String name,
        @Schema(types = {"string", "null"}) String description,
        @Schema(types = {"string", "null"}) String countryCode,
        @Schema(types = {"string", "null"}) String city,
        @Schema(types = {"string", "null"}, format = "date") LocalDate startDate,
        LocalDate targetDate,
        String currency,
        TargetMode targetMode,
        @Schema(types = {"number", "null"}) BigDecimal targetAmount,
        @Schema(types = {"number", "null"}) BigDecimal plannedMonthlySaving,
        ObjectiveStatus status,
        boolean primary,
        @Schema(types = {"string", "null"}) String color,
        @Schema(types = {"string", "null"}) String icon,
        int horizonMonths,
        ExpenseScope expenseScope) {

    public static ObjectiveResponse from(Objective o) {
        return new ObjectiveResponse(o.getId(), o.getName(), o.getDescription(), o.getCountryCode(), o.getCity(),
                o.getStartDate(), o.getTargetDate(), o.getCurrency(), o.getTargetMode(), scale2(o.getTargetAmount()),
                scale2(o.getPlannedMonthlySaving()), o.getStatus(), o.isPrimary(), o.getColor(), o.getIcon(),
                o.getHorizonMonths(), o.getExpenseScope());
    }

    private static BigDecimal scale2(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_EVEN);
    }
}
