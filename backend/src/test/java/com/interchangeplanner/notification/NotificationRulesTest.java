package com.interchangeplanner.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.interchangeplanner.notification.application.ExchangeAlertEvaluator;
import com.interchangeplanner.notification.application.LeadTime;
import com.interchangeplanner.notification.domain.ExchangeRateAlert.Condition;
import com.interchangeplanner.notification.domain.ExchangeRateAlert.State;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class NotificationRulesTest {

    static final LocalDate TODAY = LocalDate.of(2026, 10, 22);
    static final List<Integer> LEADS = List.of(30, 7, 1);

    @Test
    void antecedenciaEscolheAMenorJanelaQueContemAData() {
        assertThat(LeadTime.bucket(TODAY, TODAY.plusDays(25), LEADS)).isEqualTo(30);
        assertThat(LeadTime.bucket(TODAY, TODAY.plusDays(7), LEADS)).isEqualTo(7);
        assertThat(LeadTime.bucket(TODAY, TODAY.plusDays(5), LEADS)).isEqualTo(7);
        assertThat(LeadTime.bucket(TODAY, TODAY.plusDays(1), LEADS)).isEqualTo(1);
        assertThat(LeadTime.bucket(TODAY, TODAY, LEADS)).isEqualTo(1);
        // longe demais, no passado ou sem antecedências: nada
        assertThat(LeadTime.bucket(TODAY, TODAY.plusDays(31), LEADS)).isNull();
        assertThat(LeadTime.bucket(TODAY, TODAY.minusDays(1), LEADS)).isNull();
        assertThat(LeadTime.bucket(TODAY, TODAY, List.of())).isNull();
        assertThat(LeadTime.bucket(TODAY, TODAY, List.of(0))).isZero();
    }

    @Test
    void textoDaAntecedencia() {
        assertThat(LeadTime.phrase(TODAY, TODAY)).isEqualTo("hoje");
        assertThat(LeadTime.phrase(TODAY, TODAY.plusDays(1))).isEqualTo("amanhã");
        assertThat(LeadTime.phrase(TODAY, TODAY.plusDays(7))).isEqualTo("em 7 dias");
    }

    static ExchangeAlertEvaluator.Outcome below(State state, String rate) {
        return ExchangeAlertEvaluator.evaluate(Condition.BELOW, new BigDecimal("5.80"), state, new BigDecimal(rate));
    }

    @Test
    void alertaAbaixoDisparaUmaVezESoRearmaLongeDaMeta() {
        assertThat(below(State.ARMED, "5.85")).isEqualTo(new ExchangeAlertEvaluator.Outcome(State.ARMED, false));
        assertThat(below(State.ARMED, "5.80")).isEqualTo(new ExchangeAlertEvaluator.Outcome(State.TRIGGERED, true));
        // oscilando perto da meta: não repete
        assertThat(below(State.TRIGGERED, "5.79")).isEqualTo(new ExchangeAlertEvaluator.Outcome(State.TRIGGERED, false));
        assertThat(below(State.TRIGGERED, "5.82")).isEqualTo(new ExchangeAlertEvaluator.Outcome(State.TRIGGERED, false));
        // passou de 5,80 × 1,005 = 5,829: rearma, e o próximo cruzamento avisa de novo
        assertThat(below(State.TRIGGERED, "5.83")).isEqualTo(new ExchangeAlertEvaluator.Outcome(State.ARMED, false));
    }

    @Test
    void alertaAcimaEhSimetrico() {
        var t = new BigDecimal("6.50");
        assertThat(ExchangeAlertEvaluator.evaluate(Condition.ABOVE, t, State.ARMED, new BigDecimal("6.51")).fire()).isTrue();
        assertThat(ExchangeAlertEvaluator.evaluate(Condition.ABOVE, t, State.TRIGGERED, new BigDecimal("6.48")).state())
                .isEqualTo(State.TRIGGERED);
        assertThat(ExchangeAlertEvaluator.evaluate(Condition.ABOVE, t, State.TRIGGERED, new BigDecimal("6.46")).state())
                .isEqualTo(State.ARMED);
    }
}
