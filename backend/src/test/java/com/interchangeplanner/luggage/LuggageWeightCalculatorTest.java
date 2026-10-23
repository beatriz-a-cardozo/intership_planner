package com.interchangeplanner.luggage;

import static org.assertj.core.api.Assertions.assertThat;

import com.interchangeplanner.luggage.application.LuggageWeightCalculator;
import com.interchangeplanner.luggage.application.LuggageWeightCalculator.Line;
import com.interchangeplanner.luggage.application.LuggageWeightCalculator.State;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class LuggageWeightCalculatorTest {

    @Test
    void somaQuantidadeVezesPesoEmGramasEContaItensSemPeso() {
        var r = LuggageWeightCalculator.calculate(10_000,
                List.of(new Line(3, 450), new Line(1, 1_200), new Line(2, null)));
        assertThat(r.weightG()).isEqualTo(2_550);
        assertThat(r.itemsWithoutWeight()).isEqualTo(1);
        assertThat(r.percent()).isEqualByComparingTo(new BigDecimal("25.5"));
        assertThat(r.state()).isEqualTo(State.OK);
        assertThat(r.remainingG()).isEqualTo(7_450);
        assertThat(r.excessG()).isZero();
    }

    @Test
    void avisoAcimaDe90EAlertaAcimaDe100() {
        assertThat(LuggageWeightCalculator.calculate(10_000, List.of(new Line(1, 9_000))).state()).isEqualTo(State.OK);
        assertThat(LuggageWeightCalculator.calculate(10_000, List.of(new Line(1, 9_001))).state())
                .isEqualTo(State.WARNING);
        assertThat(LuggageWeightCalculator.calculate(10_000, List.of(new Line(1, 10_000))).state())
                .isEqualTo(State.WARNING);

        var over = LuggageWeightCalculator.calculate(5_800, List.of(new Line(2, 3_000)));
        assertThat(over.state()).isEqualTo(State.OVER);
        assertThat(over.excessG()).isEqualTo(200);
        assertThat(over.remainingG()).isZero();
        assertThat(over.percent()).isEqualByComparingTo(new BigDecimal("103.4"));
    }

    @Test
    void semLimiteNaoTemPercentualNemAlerta() {
        var r = LuggageWeightCalculator.calculate(null, List.of(new Line(1, 30_000)));
        assertThat(r.state()).isEqualTo(State.NO_LIMIT);
        assertThat(r.percent()).isNull();
        assertThat(r.remainingG()).isNull();
        assertThat(LuggageWeightCalculator.calculate(23_000, List.of()).weightG()).isZero();
    }
}
