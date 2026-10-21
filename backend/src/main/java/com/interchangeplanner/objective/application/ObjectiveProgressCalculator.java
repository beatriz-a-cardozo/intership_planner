package com.interchangeplanner.objective.application;

import com.interchangeplanner.objective.domain.TargetMode;
import com.interchangeplanner.shared.money.Money;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Regras de meta e progresso do objetivo (plano técnico §4.3). Classe pura: todos os valores chegam já
 * convertidos para a moeda do objetivo.
 *
 * <ul>
 *   <li><b>Necessidade planejada</b> = gastos no escopo + ajustes manuais (com sinal, ex.: −75.000 do Sperrkonto)</li>
 *   <li><b>Meta</b> = valor manual (MANUAL) ou max(necessidade planejada, 0) (DERIVED)</li>
 *   <li><b>Mobilizado</b> = já pago + saldo das contas vinculadas</li>
 *   <li><b>Progresso</b> = mobilizado / meta (valor real exposto; a barra limita a 100%)</li>
 *   <li><b>Faltam</b> = max(meta − mobilizado, 0)</li>
 *   <li><b>Meses restantes</b> = do mês corrente (inclusive, pois ainda não fechou) até o mês da data alvo (inclusive);
 *       data alvo no passado = 0 (objetivo vencido)</li>
 *   <li><b>Economia mensal necessária</b> = faltam / meses restantes (ou tudo agora, se vencido)</li>
 * </ul>
 */
public final class ObjectiveProgressCalculator {

    public record Inputs(
            String currency,
            TargetMode targetMode,
            Money manualTarget,
            Money plannedExpenses,
            Money adjustments,
            Money paid,
            Money accountsBalance,
            LocalDate targetDate,
            LocalDate today,
            Money plannedMonthlySaving) {

        public Inputs {
            Objects.requireNonNull(currency);
            Objects.requireNonNull(targetMode);
            Objects.requireNonNull(plannedExpenses);
            Objects.requireNonNull(adjustments);
            Objects.requireNonNull(paid);
            Objects.requireNonNull(accountsBalance);
            Objects.requireNonNull(targetDate);
            Objects.requireNonNull(today);
            if (targetMode == TargetMode.MANUAL && manualTarget == null) {
                throw new IllegalArgumentException("Meta manual sem valor");
            }
        }
    }

    /**
     * @param ratio           mobilizado/meta sem limite (1.25 = 125%)
     * @param percentComplete 0–100, para a barra/anel
     * @param onTrack         economia mensal planejada cobre a necessária; nulo se não houver planejamento
     */
    public record Result(
            Money plannedNeed,
            Money target,
            Money mobilized,
            Money remaining,
            BigDecimal ratio,
            BigDecimal percentComplete,
            int monthsRemaining,
            Money monthlySavingNeeded,
            boolean overdue,
            Boolean onTrack) {
    }

    private ObjectiveProgressCalculator() {
    }

    public static Result calculate(Inputs in) {
        Money plannedNeed = in.plannedExpenses().plus(in.adjustments());
        Money target = in.targetMode() == TargetMode.MANUAL ? in.manualTarget() : plannedNeed.atLeastZero();
        Money mobilized = in.paid().plus(in.accountsBalance());
        Money remaining = target.minus(mobilized).atLeastZero();

        BigDecimal ratio = ratio(mobilized, target);
        BigDecimal percent = ratio.min(BigDecimal.ONE).max(BigDecimal.ZERO)
                .multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_EVEN);

        boolean overdue = in.targetDate().isBefore(in.today());
        int months = monthsRemaining(in.today(), in.targetDate());
        Money monthly = months == 0 ? remaining : remaining.dividedBy(months);
        Boolean onTrack = in.plannedMonthlySaving() == null ? null : in.plannedMonthlySaving().compareTo(monthly) >= 0;

        return new Result(plannedNeed, target, mobilized, remaining, ratio, percent, months, monthly, overdue, onTrack);
    }

    static int monthsRemaining(LocalDate today, LocalDate targetDate) {
        if (targetDate.isBefore(today)) {
            return 0;
        }
        return (int) ChronoUnit.MONTHS.between(YearMonth.from(today), YearMonth.from(targetDate)) + 1;
    }

    private static BigDecimal ratio(Money mobilized, Money target) {
        if (target.isZero()) {
            // Sem meta (ainda sem gastos): qualquer valor mobilizado já "cobre" a meta
            return mobilized.isPositive() ? BigDecimal.ONE.setScale(4) : BigDecimal.ZERO.setScale(4);
        }
        return mobilized.amount().divide(target.amount(), 4, RoundingMode.HALF_EVEN);
    }
}
