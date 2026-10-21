package com.interchangeplanner.finance.account.api;

import com.interchangeplanner.exchange.api.ExchangeRateResponse;
import com.interchangeplanner.shared.money.MoneyResponse;
import java.util.List;

/**
 * Saldos por moeda (nunca misturados) e o total convertido apenas como visão.
 *
 * @param byCurrency soma das contas ativas em cada moeda original
 * @param total      soma convertida para a moeda principal do usuário
 */
public record AccountsSummaryResponse(
        List<MoneyResponse> byCurrency,
        MoneyResponse total,
        int accountCount,
        List<ExchangeRateResponse> exchangeRates) {
}
