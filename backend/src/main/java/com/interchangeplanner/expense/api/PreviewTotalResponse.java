package com.interchangeplanner.expense.api;

import com.interchangeplanner.shared.money.MoneyResponse;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * @param occurrences          ocorrências usadas no cálculo
 * @param suggestedOccurrences sugestão pelo horizonte do objetivo (nula para CUSTOM)
 * @param totalConverted       total na moeda do objetivo
 */
public record PreviewTotalResponse(
        int occurrences,
        @Schema(types = {"integer", "null"}) Integer suggestedOccurrences,
        int horizonMonths,
        MoneyResponse total,
        MoneyResponse totalConverted) {
}
