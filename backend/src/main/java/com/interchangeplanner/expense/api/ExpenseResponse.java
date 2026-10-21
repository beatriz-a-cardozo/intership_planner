package com.interchangeplanner.expense.api;

import com.interchangeplanner.expense.domain.ExpenseStatus;
import com.interchangeplanner.expense.domain.Recurrence;
import com.interchangeplanner.shared.money.MoneyResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Gasto com valores calculados no backend (o frontend só formata).
 *
 * @param total          preço × quantidade × ocorrências, na moeda original
 * @param totalConverted total na moeda do objetivo (cotação vigente do usuário)
 * @param paid           soma dos pagamentos na moeda do gasto (taxa de cada pagamento)
 * @param remaining      max(total − pago, 0)
 */
public record ExpenseResponse(
        UUID id,
        UUID objectiveId,
        String name,
        @Schema(types = {"string", "null"}) String description,
        CatalogRef category,
        @Schema(types = {"object", "null"}) CatalogRef cycle,
        String currency,
        BigDecimal unitPrice,
        int quantity,
        Recurrence recurrence,
        int occurrences,
        MoneyResponse total,
        MoneyResponse totalConverted,
        MoneyResponse paid,
        MoneyResponse remaining,
        ExpenseStatus status,
        boolean required,
        boolean refundable,
        @Schema(types = {"string", "null"}, format = "date") LocalDate dueDate,
        @Schema(types = {"string", "null"}, format = "date") LocalDate endDate,
        @Schema(types = {"string", "null"}) String notes) {

    /** Referência resumida a categoria/ciclo (nome, cor e ícone para exibição). */
    public record CatalogRef(UUID id, String name, String color, String icon) {
    }
}
