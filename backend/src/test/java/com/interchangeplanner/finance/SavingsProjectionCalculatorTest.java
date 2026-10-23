package com.interchangeplanner.finance;

import static org.assertj.core.api.Assertions.assertThat;

import com.interchangeplanner.finance.ledger.application.SavingsProjectionCalculator;
import com.interchangeplanner.finance.ledger.application.SavingsProjectionCalculator.Inputs;
import com.interchangeplanner.finance.ledger.application.SavingsProjectionCalculator.Result;
import com.interchangeplanner.shared.money.Money;
import java.time.YearMonth;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SavingsProjectionCalculatorTest {

    static final YearMonth NOW = YearMonth.of(2026, 10);
    static final YearMonth TARGET = YearMonth.of(2027, 5);

    static Money brl(String v) {
        return Money.of(v, "BRL");
    }

    static Inputs inputs(String target, String mobilized, String baseline, Map<YearMonth, Money> income,
            Map<YearMonth, Money> expenses) {
        return new Inputs(brl(target), brl(mobilized), NOW, TARGET, brl(baseline), income, expenses, 36);
    }

    @Test
    void alcancaAntesDaDataAlvo() {
        // 10.000 + 7 × 3.000 = 31.000 em mai/2027; meta 25.000 alcançada em mar/2027 (10.000 + 5 × 3.000)
        Result r = SavingsProjectionCalculator.project(inputs("25000", "10000", "3000", Map.of(), Map.of()));
        assertThat(r.reachMonth()).isEqualTo(YearMonth.of(2027, 3));
        assertThat(r.reachesByTarget()).isTrue();
        assertThat(r.deficitAtTarget().isZero()).isTrue();
        assertThat(r.months()).hasSize(7);
        assertThat(r.months().getFirst().month()).isEqualTo(YearMonth.of(2026, 11));
    }

    @Test
    void parcelasComprometidasReduzemAEconomiaDoMes() {
        Map<YearMonth, Money> installments = Map.of(YearMonth.of(2026, 11), brl("1000"), YearMonth.of(2026, 12), brl("1000"));
        Result r = SavingsProjectionCalculator.project(inputs("100000", "0", "3000", Map.of(), installments));
        assertThat(r.months().getFirst().expectedSaving()).isEqualTo(brl("2000"));
        assertThat(r.months().get(2).expectedSaving()).isEqualTo(brl("3000"));
    }

    @Test
    void naoAlcancaInformaDeficitNaDataAlvoEMesFuturoQueAlcanca() {
        // 7 meses × 2.000 = 14.000 até mai/2027 → faltam 6.000; alcança 20.000 em ago/2027 (10 × 2.000)
        Result r = SavingsProjectionCalculator.project(inputs("20000", "0", "2000", Map.of(), Map.of()));
        assertThat(r.reachesByTarget()).isFalse();
        assertThat(r.deficitAtTarget()).isEqualTo(brl("6000"));
        assertThat(r.reachMonth()).isEqualTo(YearMonth.of(2027, 8));
    }

    @Test
    void salarioRecorrenteEntraPeloValorProgramado() {
        Map<YearMonth, Money> salary = Map.of(YearMonth.of(2026, 11), brl("5000"));
        Result r = SavingsProjectionCalculator.project(inputs("100000", "0", "-1000", salary, Map.of()));
        assertThat(r.months().getFirst().expectedSaving()).isEqualTo(brl("4000"));
        assertThat(r.months().get(1).expectedSaving()).isEqualTo(brl("-1000"));
    }

    @Test
    void jaAlcancadaEhOMesAtual() {
        Result r = SavingsProjectionCalculator.project(inputs("5000", "6000", "0", Map.of(), Map.of()));
        assertThat(r.reachMonth()).isEqualTo(NOW);
        assertThat(r.reachesByTarget()).isTrue();
    }

    @Test
    void nuncaAlcancaNoHorizonte() {
        Result r = SavingsProjectionCalculator.project(inputs("1000000", "0", "100", Map.of(), Map.of()));
        assertThat(r.reachMonth()).isNull();
        assertThat(r.months()).hasSize(36);
        assertThat(r.deficitAtTarget()).isEqualTo(brl("999300"));
    }
}
