package com.interchangeplanner.finance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.interchangeplanner.finance.ledger.application.InstallmentCalculator;
import com.interchangeplanner.finance.ledger.application.InstallmentCalculator.Installment;
import com.interchangeplanner.shared.money.Money;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class InstallmentCalculatorTest {

    static Money brl(String v) {
        return Money.of(v, "BRL");
    }

    @Test
    void ultimaParcelaAbsorveOsCentavos() {
        List<Installment> parts = InstallmentCalculator.schedule(brl("100"), 3, LocalDate.of(2026, 11, 10));
        assertThat(parts).extracting(Installment::amount).containsExactly(brl("33.33"), brl("33.33"), brl("33.34"));
        assertThat(parts.stream().map(Installment::amount).reduce(Money::plus).orElseThrow()).isEqualTo(brl("100"));
    }

    @Test
    void parcelamentoDaAgenciaEm10x() {
        // "Obs. Parcelado em 10x" na planilha
        List<Installment> parts = InstallmentCalculator.schedule(brl("9500"), 10, LocalDate.of(2026, 11, 5));
        assertThat(parts).hasSize(10).allSatisfy(p -> assertThat(p.amount()).isEqualTo(brl("950")));
        assertThat(parts.getLast().number()).isEqualTo(10);
        assertThat(parts.getLast().dueDate()).isEqualTo(LocalDate.of(2027, 8, 5));
    }

    @Test
    void vencimentoDia31NaoEscorregaDepoisDeFevereiro() {
        List<Installment> parts = InstallmentCalculator.schedule(brl("400"), 4, LocalDate.of(2027, 1, 31));
        assertThat(parts).extracting(Installment::dueDate).containsExactly(
                LocalDate.of(2027, 1, 31), LocalDate.of(2027, 2, 28), LocalDate.of(2027, 3, 31), LocalDate.of(2027, 4, 30));
    }

    @Test
    void totalAPartirDoValorDaParcela() {
        assertThat(InstallmentCalculator.totalFromInstallment(brl("219.80"), 10)).isEqualTo(brl("2198"));
    }

    @Test
    void parcelaUnicaEValidacoes() {
        assertThat(InstallmentCalculator.schedule(brl("10.01"), 1, LocalDate.of(2026, 1, 1)))
                .extracting(Installment::amount).containsExactly(brl("10.01"));
        assertThatThrownBy(() -> InstallmentCalculator.schedule(brl("10"), 0, LocalDate.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parcelamentoEmAndamentoGeraSoAsQueFaltamAPartirDaProxima() {
        // 10x de R$ 219,80, já pagas 3: as parcelas 4 a 10 vencem a partir de 05/11
        InstallmentCalculator.Schedule s = InstallmentCalculator.inProgress(brl("2198"), 10, 3, LocalDate.of(2026, 11, 5));
        assertThat(s.paidBefore()).isEqualTo(brl("659.40"));
        assertThat(s.open()).hasSize(7);
        assertThat(s.open().getFirst()).isEqualTo(new Installment(4, LocalDate.of(2026, 11, 5), brl("219.80")));
        assertThat(s.open().getLast()).isEqualTo(new Installment(10, LocalDate.of(2027, 5, 5), brl("219.80")));
    }

    @Test
    void emAndamentoMantemOsCentavosNaUltimaEADataAncoradaNaProxima() {
        // R$ 100 em 3x, 1 já paga: 33,33 antes; restam 33,33 e 33,34. Próxima dia 31/01 → 28/02 sem escorregar
        InstallmentCalculator.Schedule s = InstallmentCalculator.inProgress(brl("100"), 3, 1, LocalDate.of(2027, 1, 31));
        assertThat(s.paidBefore()).isEqualTo(brl("33.33"));
        assertThat(s.open()).extracting(Installment::amount).containsExactly(brl("33.33"), brl("33.34"));
        assertThat(s.open()).extracting(Installment::dueDate)
                .containsExactly(LocalDate.of(2027, 1, 31), LocalDate.of(2027, 2, 28));
        assertThatThrownBy(() -> InstallmentCalculator.inProgress(brl("100"), 3, 3, LocalDate.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
