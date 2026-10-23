package com.interchangeplanner.finance.ledger.application;

import com.interchangeplanner.finance.ledger.domain.Frequency;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Datas de ocorrência de uma recorrência (assinatura, conta fixa, salário). Classe pura.
 *
 * <ul>
 *   <li>Semanal: a cada 7 dias a partir do início.</li>
 *   <li>Mensal / a cada N meses / anual: no dia de cobrança (padrão = dia do início); em meses mais curtos,
 *       o último dia do mês (dia 31 → 30/04, 28/02). Nunca antes da data de início.</li>
 *   <li>Respeita a data final (inclusive).</li>
 * </ul>
 */
public final class RecurrenceScheduler {

    public record Rule(Frequency frequency, Integer intervalMonths, Integer billingDay, LocalDate startDate,
            LocalDate endDate) {

        public Rule {
            Objects.requireNonNull(frequency);
            Objects.requireNonNull(startDate);
            if (frequency == Frequency.EVERY_N_MONTHS && (intervalMonths == null || intervalMonths < 1)) {
                throw new IllegalArgumentException("Intervalo em meses obrigatório");
            }
        }

        int stepMonths() {
            return switch (frequency) {
                case MONTHLY -> 1;
                case YEARLY -> 12;
                case EVERY_N_MONTHS -> intervalMonths;
                case WEEKLY -> throw new IllegalStateException("Semanal não usa meses");
            };
        }

        int day() {
            return billingDay != null ? billingDay : startDate.getDayOfMonth();
        }
    }

    /** Safety net contra regras degeneradas (ex.: semanal por décadas). */
    static final int MAX_OCCURRENCES = 1_000;

    private RecurrenceScheduler() {
    }

    /** Ocorrências dentro de [from, to] (inclusive), em ordem. */
    public static List<LocalDate> occurrences(Rule rule, LocalDate from, LocalDate to) {
        LocalDate end = rule.endDate() != null && rule.endDate().isBefore(to) ? rule.endDate() : to;
        List<LocalDate> result = new ArrayList<>();
        if (end.isBefore(rule.startDate()) || end.isBefore(from)) {
            return result;
        }
        if (rule.frequency() == Frequency.WEEKLY) {
            for (LocalDate d = rule.startDate(); !d.isAfter(end) && result.size() < MAX_OCCURRENCES; d = d.plusWeeks(1)) {
                if (!d.isBefore(from)) {
                    result.add(d);
                }
            }
            return result;
        }
        YearMonth month = YearMonth.from(rule.startDate());
        LocalDate first = clamp(month, rule.day());
        if (first.isBefore(rule.startDate())) {
            // dia de cobrança já passou no mês de início: começa no próximo ciclo
            month = month.plusMonths(rule.stepMonths());
        }
        while (result.size() < MAX_OCCURRENCES) {
            LocalDate d = clamp(month, rule.day());
            if (d.isAfter(end)) {
                break;
            }
            if (!d.isBefore(from)) {
                result.add(d);
            }
            month = month.plusMonths(rule.stepMonths());
        }
        return result;
    }

    private static LocalDate clamp(YearMonth month, int day) {
        return month.atDay(Math.min(day, month.lengthOfMonth()));
    }
}
