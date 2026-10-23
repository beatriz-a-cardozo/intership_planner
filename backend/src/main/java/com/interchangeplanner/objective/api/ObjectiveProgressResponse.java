package com.interchangeplanner.objective.api;

import com.interchangeplanner.exchange.api.ExchangeRateResponse;
import com.interchangeplanner.objective.domain.ExpenseScope;
import com.interchangeplanner.objective.domain.TargetMode;
import com.interchangeplanner.shared.money.MoneyResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Meta e progresso do objetivo (todos os valores na moeda do objetivo). Ver ObjectiveProgressCalculator.
 *
 * @param plannedExpenses     gastos no escopo (não cancelados)
 * @param adjustments         ajustes manuais (com sinal)
 * @param plannedNeed         plannedExpenses + adjustments
 * @param target              meta efetiva (manual ou derivada)
 * @param paid                já pago dos gastos no escopo
 * @param accountsBalance     saldo das contas vinculadas ao objetivo
 * @param mobilized           paid + accountsBalance
 * @param ratio               mobilizado/meta sem limite (1.25 = 125%)
 * @param percentComplete     0–100 para a barra
 * @param monthlySavingNeeded quanto guardar por mês para atingir a meta na data alvo
 * @param onTrack             economia mensal planejada cobre a necessária (nulo se não planejada)
 * @param scopeGoals          as duas metas derivadas lado a lado: só obrigatórios e obrigatórios + opcionais
 */
public record ObjectiveProgressResponse(
        UUID objectiveId,
        String currency,
        TargetMode targetMode,
        ExpenseScope expenseScope,
        MoneyResponse plannedExpenses,
        MoneyResponse adjustments,
        MoneyResponse plannedNeed,
        MoneyResponse target,
        MoneyResponse paid,
        MoneyResponse accountsBalance,
        MoneyResponse mobilized,
        MoneyResponse remaining,
        BigDecimal ratio,
        BigDecimal percentComplete,
        LocalDate targetDate,
        int monthsRemaining,
        MoneyResponse monthlySavingNeeded,
        @Schema(types = {"object", "null"}) MoneyResponse plannedMonthlySaving,
        @Schema(types = {"boolean", "null"}) Boolean onTrack,
        boolean overdue,
        List<ExchangeRateResponse> exchangeRates,
        List<ScopeGoal> scopeGoals) {

    /**
     * Meta derivada de um escopo de gastos (com os ajustes manuais), independente do modo configurado.
     *
     * @param plannedExpenses gastos do escopo (não cancelados)
     * @param target          max(gastos + ajustes, 0)
     * @param mobilized       já pago dos gastos do escopo + saldo das contas vinculadas
     * @param configured      é o escopo configurado no objetivo (o que define a meta quando ela é derivada)
     */
    public record ScopeGoal(
            ExpenseScope scope,
            MoneyResponse plannedExpenses,
            MoneyResponse target,
            MoneyResponse mobilized,
            MoneyResponse remaining,
            BigDecimal ratio,
            BigDecimal percentComplete,
            MoneyResponse monthlySavingNeeded,
            boolean configured) {
    }
}
