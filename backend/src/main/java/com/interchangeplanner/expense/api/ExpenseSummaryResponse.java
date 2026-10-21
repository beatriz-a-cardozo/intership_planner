package com.interchangeplanner.expense.api;

import com.interchangeplanner.exchange.api.ExchangeRateResponse;
import com.interchangeplanner.shared.money.MoneyResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

/**
 * Resumo dos gastos de um objetivo, tudo convertido para a moeda do objetivo. Gastos cancelados ficam fora.
 * Substitui as agregações SUMIF da aba "Auxiliares" e os indicadores do DASHBOARD da planilha.
 */
public record ExpenseSummaryResponse(
        UUID objectiveId,
        String currency,
        Totals totals,
        GroupBy groupBy,
        List<Group> groups,
        List<ExchangeRateResponse> exchangeRates) {

    public enum GroupBy {
        CYCLE,
        CATEGORY,
        RECURRENCE,
        STATUS
    }

    /**
     * @param total                    soma de todos os gastos não cancelados
     * @param required                 só os obrigatórios
     * @param optional                 só os não obrigatórios
     * @param paid                     total já pago
     * @param remaining                Σ max(total − pago, 0) de cada gasto (mesmo escopo do total)
     * @param adjustments              Σ ajustes manuais do objetivo (com sinal; ex.: −75.000)
     * @param totalAfterAdjustments    total + ajustes
     * @param requiredAfterAdjustments obrigatórios + ajustes (o "Total Obrigatório" da planilha)
     * @param monthlyRecurring         custo de um mês dos gastos mensais (preço × quantidade)
     */
    public record Totals(
            MoneyResponse total,
            MoneyResponse required,
            MoneyResponse optional,
            MoneyResponse paid,
            MoneyResponse remaining,
            MoneyResponse adjustments,
            MoneyResponse totalAfterAdjustments,
            MoneyResponse requiredAfterAdjustments,
            MoneyResponse monthlyRecurring,
            int count,
            int cancelledCount) {
    }

    /**
     * @param key   id da categoria/ciclo, nome do enum (periodicidade/status) ou "none" (sem ciclo)
     * @param color cor para os gráficos (nula para agrupamentos por enum)
     */
    public record Group(
            String key,
            String label,
            @Schema(types = {"string", "null"}) String color,
            @Schema(types = {"string", "null"}) String icon,
            MoneyResponse total,
            MoneyResponse required,
            MoneyResponse paid,
            MoneyResponse remaining,
            int count) {
    }
}
