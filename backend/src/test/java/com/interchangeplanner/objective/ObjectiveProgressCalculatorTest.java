package com.interchangeplanner.objective;

import static org.assertj.core.api.Assertions.assertThat;

import com.interchangeplanner.objective.application.ObjectiveProgressCalculator;
import com.interchangeplanner.objective.application.ObjectiveProgressCalculator.Inputs;
import com.interchangeplanner.objective.application.ObjectiveProgressCalculator.Result;
import com.interchangeplanner.objective.domain.TargetMode;
import com.interchangeplanner.shared.money.Money;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ObjectiveProgressCalculatorTest {

    static final LocalDate TODAY = LocalDate.of(2026, 10, 21);
    static final LocalDate TARGET = LocalDate.of(2027, 5, 15);

    static Money brl(String v) {
        return Money.of(v, "BRL");
    }

    static Inputs derived(String expenses, String adjustments, String paid, String accounts) {
        return new Inputs("BRL", TargetMode.DERIVED, null, brl(expenses), brl(adjustments), brl(paid), brl(accounts),
                TARGET, TODAY, null);
    }

    @Test
    void metaDerivadaDescontaOSperrkontoUmaUnicaVez() {
        // Regressão do plano: obrigatórios em 24 meses = 275.793,28; com o Sperrkonto (−75.000) = 200.793,28
        Result r = ObjectiveProgressCalculator.calculate(derived("275793.28", "-75000", "11721.68", "0"));
        assertThat(r.plannedNeed()).isEqualTo(brl("200793.28"));
        assertThat(r.target()).isEqualTo(brl("200793.28"));
        assertThat(r.mobilized()).isEqualTo(brl("11721.68"));
        assertThat(r.remaining()).isEqualTo(brl("189071.60"));
    }

    @Test
    void mobilizadoSomaPagoESaldoDasContas() {
        Result r = ObjectiveProgressCalculator.calculate(derived("100000", "0", "10000", "15000"));
        assertThat(r.mobilized()).isEqualTo(brl("25000"));
        assertThat(r.ratio()).isEqualByComparingTo("0.25");
        assertThat(r.percentComplete()).isEqualByComparingTo("25.0");
    }

    @Test
    void mesesRestantesContamOMesCorrenteEOMesAlvo() {
        // out/2026 .. mai/2027 = 8 meses
        Result r = ObjectiveProgressCalculator.calculate(derived("80000", "0", "0", "0"));
        assertThat(r.monthsRemaining()).isEqualTo(8);
        assertThat(r.monthlySavingNeeded()).isEqualTo(brl("10000"));
        assertThat(r.overdue()).isFalse();
    }

    @Test
    void viradaDeAnoEDataAlvoNoMesCorrente() {
        Inputs sameMonth = new Inputs("BRL", TargetMode.DERIVED, null, brl("900"), brl("0"), brl("0"), brl("0"),
                LocalDate.of(2026, 10, 31), TODAY, null);
        assertThat(ObjectiveProgressCalculator.calculate(sameMonth).monthsRemaining()).isEqualTo(1);

        Inputs nextJanuary = new Inputs("BRL", TargetMode.DERIVED, null, brl("900"), brl("0"), brl("0"), brl("0"),
                LocalDate.of(2027, 1, 1), TODAY, null);
        assertThat(ObjectiveProgressCalculator.calculate(nextJanuary).monthsRemaining()).isEqualTo(4);
    }

    @Test
    void objetivoVencidoPedeTudoAgora() {
        Inputs past = new Inputs("BRL", TargetMode.DERIVED, null, brl("5000"), brl("0"), brl("1000"), brl("0"),
                LocalDate.of(2026, 9, 1), TODAY, null);
        Result r = ObjectiveProgressCalculator.calculate(past);
        assertThat(r.overdue()).isTrue();
        assertThat(r.monthsRemaining()).isZero();
        assertThat(r.monthlySavingNeeded()).isEqualTo(brl("4000"));
    }

    @Test
    void metaManualIgnoraOsGastos() {
        Inputs manual = new Inputs("BRL", TargetMode.MANUAL, brl("150000"), brl("999999"), brl("-75000"), brl("0"),
                brl("30000"), TARGET, TODAY, null);
        Result r = ObjectiveProgressCalculator.calculate(manual);
        assertThat(r.target()).isEqualTo(brl("150000"));
        assertThat(r.remaining()).isEqualTo(brl("120000"));
    }

    @Test
    void progressoAcimaDe100PorCentoEhExpostoMasABarraLimita() {
        Result r = ObjectiveProgressCalculator.calculate(derived("10000", "0", "0", "12500"));
        assertThat(r.ratio()).isEqualByComparingTo("1.25");
        assertThat(r.percentComplete()).isEqualByComparingTo("100.0");
        assertThat(r.remaining().isZero()).isTrue();
    }

    @Test
    void ajustesMaioresQueOsGastosNaoGeramMetaNegativa() {
        Result r = ObjectiveProgressCalculator.calculate(derived("50000", "-75000", "0", "0"));
        assertThat(r.plannedNeed()).isEqualTo(brl("-25000"));
        assertThat(r.target().isZero()).isTrue();
        assertThat(r.ratio()).isEqualByComparingTo("0");
    }

    @Test
    void comparaEconomiaPlanejadaComANecessaria() {
        Inputs onTrack = new Inputs("BRL", TargetMode.DERIVED, null, brl("80000"), brl("0"), brl("0"), brl("0"),
                TARGET, TODAY, brl("10000"));
        Inputs behind = new Inputs("BRL", TargetMode.DERIVED, null, brl("80000"), brl("0"), brl("0"), brl("0"),
                TARGET, TODAY, brl("9999.99"));
        assertThat(ObjectiveProgressCalculator.calculate(onTrack).onTrack()).isTrue();
        assertThat(ObjectiveProgressCalculator.calculate(behind).onTrack()).isFalse();
        assertThat(ObjectiveProgressCalculator.calculate(derived("1", "0", "0", "0")).onTrack()).isNull();
    }
}
