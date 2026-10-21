package com.interchangeplanner.finance.account.api;

import com.interchangeplanner.finance.account.domain.AccountType;
import com.interchangeplanner.shared.money.MoneyResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.UUID;

/**
 * @param balance      saldo mais recente (zero se nunca informado)
 * @param balanceAsOf  data do saldo mais recente (nula se nunca informado)
 */
public record AccountResponse(
        UUID id,
        String name,
        String currency,
        AccountType type,
        @Schema(types = {"string", "null"}, format = "uuid") UUID objectiveId,
        boolean archived,
        MoneyResponse balance,
        @Schema(types = {"string", "null"}, format = "date") LocalDate balanceAsOf) {
}
