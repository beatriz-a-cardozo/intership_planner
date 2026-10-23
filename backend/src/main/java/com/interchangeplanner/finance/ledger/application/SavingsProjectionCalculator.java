package com.interchangeplanner.finance.ledger.application;

import com.interchangeplanner.shared.money.Money;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Previsão de quando o objetivo é atingido (plano técnico §4.3). Classe pura; tudo na moeda do objetivo.
 *
 * <p>Para cada mês futuro (a partir do próximo; o mês corrente já está refletido nos saldos):</p>
 * <pre>economia(m) = média das sobras AVULSAS confirmadas dos últimos meses
 *              + receitas programadas(m) − despesas programadas(m)</pre>
 * <p>"Programadas" = parcelas e recorrências (salário, aluguel, assinaturas), que entram mês a mês pelo valor
 * real em vez da média. O acumulado parte do mobilizado atual. O resultado é o primeiro mês em que o acumulado
 * alcança a meta, ou o déficit na data alvo.</p>
 */
public final class SavingsProjectionCalculator {

    public record Inputs(Money target, Money mobilized, YearMonth currentMonth, YearMonth targetMonth,
            Money baselineMonthlySaving, Map<YearMonth, Money> plannedIncome, Map<YearMonth, Money> plannedExpenses,
            int maxMonths) {

        public Inputs {
            Objects.requireNonNull(target);
            Objects.requireNonNull(mobilized);
            Objects.requireNonNull(baselineMonthlySaving);
            if (maxMonths < 1) {
                throw new IllegalArgumentException("maxMonths >= 1");
            }
        }
    }

    public record Month(YearMonth month, Money plannedIncome, Money plannedExpenses, Money expectedSaving,
            Money cumulative) {
    }

    /**
     * @param months          meses projetados (até a data alvo; se a meta não for alcançada até lá, continua
     *                        até alcançar ou até o limite)
     * @param reachMonth      primeiro mês com acumulado ≥ meta (o mês atual, se já alcançada); nulo se não alcança
     * @param deficitAtTarget quanto falta no mês alvo (zero se alcança até lá)
     */
    public record Result(List<Month> months, YearMonth reachMonth, boolean reachesByTarget, Money deficitAtTarget) {
    }

    private SavingsProjectionCalculator() {
    }

    public static Result project(Inputs in) {
        String currency = in.target().currency();
        Money cumulative = in.mobilized();
        YearMonth reach = cumulative.compareTo(in.target()) >= 0 ? in.currentMonth() : null;
        Money cumulativeAtTarget = in.targetMonth().isAfter(in.currentMonth()) ? null : cumulative;
        List<Month> months = new ArrayList<>();

        YearMonth last = in.currentMonth().plusMonths(in.maxMonths());
        for (YearMonth m = in.currentMonth().plusMonths(1); !m.isAfter(last); m = m.plusMonths(1)) {
            boolean pastTarget = m.isAfter(in.targetMonth());
            if (pastTarget && reach != null) {
                break;
            }
            Money income = in.plannedIncome().getOrDefault(m, Money.zero(currency));
            Money expenses = in.plannedExpenses().getOrDefault(m, Money.zero(currency));
            Money saving = in.baselineMonthlySaving().plus(income).minus(expenses);
            cumulative = cumulative.plus(saving);
            months.add(new Month(m, income, expenses, saving, cumulative));
            if (reach == null && cumulative.compareTo(in.target()) >= 0) {
                reach = m;
            }
            if (m.equals(in.targetMonth())) {
                cumulativeAtTarget = cumulative;
            }
        }
        if (cumulativeAtTarget == null) {
            cumulativeAtTarget = cumulative;
        }
        boolean reachesByTarget = reach != null && !reach.isAfter(in.targetMonth());
        Money deficit = reachesByTarget ? Money.zero(currency) : in.target().minus(cumulativeAtTarget).atLeastZero();
        return new Result(months, reach, reachesByTarget, deficit);
    }
}
