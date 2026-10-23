package com.interchangeplanner.notification.application;

import com.interchangeplanner.notification.domain.ExchangeRateAlert.Condition;
import com.interchangeplanner.notification.domain.ExchangeRateAlert.State;
import java.math.BigDecimal;

/**
 * Alerta de câmbio com histerese. Classe pura.
 *
 * <ul>
 *   <li>ARMED e a condição vale (cotação ≤ meta em BELOW, ≥ meta em ABOVE) → dispara e vai para TRIGGERED.</li>
 *   <li>TRIGGERED só volta a ARMED quando a cotação se afasta da meta pela margem (0,5%): evita um aviso por
 *       atualização enquanto o euro oscila em volta da meta.</li>
 * </ul>
 */
public final class ExchangeAlertEvaluator {

    /** Margem para rearmar (0,5%). */
    public static final BigDecimal REARM_MARGIN = new BigDecimal("0.005");

    public record Outcome(State state, boolean fire) {
    }

    private ExchangeAlertEvaluator() {
    }

    public static Outcome evaluate(Condition condition, BigDecimal threshold, State state, BigDecimal rate) {
        boolean met = condition == Condition.BELOW ? rate.compareTo(threshold) <= 0 : rate.compareTo(threshold) >= 0;
        if (state == State.ARMED) {
            return met ? new Outcome(State.TRIGGERED, true) : new Outcome(State.ARMED, false);
        }
        BigDecimal rearmAt = condition == Condition.BELOW
                ? threshold.multiply(BigDecimal.ONE.add(REARM_MARGIN))
                : threshold.multiply(BigDecimal.ONE.subtract(REARM_MARGIN));
        boolean away = condition == Condition.BELOW ? rate.compareTo(rearmAt) > 0 : rate.compareTo(rearmAt) < 0;
        return new Outcome(away ? State.ARMED : State.TRIGGERED, false);
    }
}
