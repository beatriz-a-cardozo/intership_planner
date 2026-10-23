package com.interchangeplanner.finance;

import static org.assertj.core.api.Assertions.assertThat;

import com.interchangeplanner.finance.ledger.application.RecurrenceScheduler;
import com.interchangeplanner.finance.ledger.application.RecurrenceScheduler.Rule;
import com.interchangeplanner.finance.ledger.domain.Frequency;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class RecurrenceSchedulerTest {

    static LocalDate d(int y, int m, int day) {
        return LocalDate.of(y, m, day);
    }

    @Test
    void mensalNoDiaDeCobrancaComMesesCurtos() {
        Rule rule = new Rule(Frequency.MONTHLY, null, 31, d(2026, 12, 1), null);
        assertThat(RecurrenceScheduler.occurrences(rule, d(2026, 12, 1), d(2027, 4, 30))).containsExactly(
                d(2026, 12, 31), d(2027, 1, 31), d(2027, 2, 28), d(2027, 3, 31), d(2027, 4, 30));
    }

    @Test
    void diaDeCobrancaJaPassadoNoMesDeInicioComecaNoProximo() {
        Rule rule = new Rule(Frequency.MONTHLY, null, 5, d(2026, 10, 20), null);
        assertThat(RecurrenceScheduler.occurrences(rule, d(2026, 10, 1), d(2026, 12, 31)))
                .containsExactly(d(2026, 11, 5), d(2026, 12, 5));
    }

    @Test
    void semDiaDeCobrancaUsaODiaDoInicio() {
        Rule rule = new Rule(Frequency.MONTHLY, null, null, d(2026, 10, 15), null);
        assertThat(RecurrenceScheduler.occurrences(rule, d(2026, 10, 1), d(2026, 11, 30)))
                .containsExactly(d(2026, 10, 15), d(2026, 11, 15));
    }

    @Test
    void semanalAnualEACadaNMesesAtravessandoOAno() {
        assertThat(RecurrenceScheduler.occurrences(new Rule(Frequency.WEEKLY, null, null, d(2026, 12, 24), null),
                d(2026, 12, 1), d(2027, 1, 10))).containsExactly(d(2026, 12, 24), d(2026, 12, 31), d(2027, 1, 7));
        assertThat(RecurrenceScheduler.occurrences(new Rule(Frequency.YEARLY, null, null, d(2024, 2, 29), null),
                d(2025, 1, 1), d(2028, 12, 31))).containsExactly(d(2025, 2, 28), d(2026, 2, 28), d(2027, 2, 28), d(2028, 2, 29));
        assertThat(RecurrenceScheduler.occurrences(new Rule(Frequency.EVERY_N_MONTHS, 3, 10, d(2026, 11, 1), null),
                d(2026, 11, 1), d(2027, 8, 31))).containsExactly(d(2026, 11, 10), d(2027, 2, 10), d(2027, 5, 10), d(2027, 8, 10));
    }

    @Test
    void respeitaDataFinalEJanela() {
        Rule rule = new Rule(Frequency.MONTHLY, null, 10, d(2026, 1, 10), d(2026, 3, 10));
        assertThat(RecurrenceScheduler.occurrences(rule, d(2026, 2, 1), d(2026, 12, 31)))
                .containsExactly(d(2026, 2, 10), d(2026, 3, 10));
        assertThat(RecurrenceScheduler.occurrences(rule, d(2026, 4, 1), d(2026, 12, 31))).isEmpty();
    }
}
