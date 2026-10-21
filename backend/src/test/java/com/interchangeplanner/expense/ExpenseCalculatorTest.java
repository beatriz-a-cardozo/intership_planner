package com.interchangeplanner.expense;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.interchangeplanner.expense.application.ExpenseCalculator;
import com.interchangeplanner.expense.domain.ExpenseStatus;
import com.interchangeplanner.expense.domain.Recurrence;
import com.interchangeplanner.shared.error.BusinessRuleException;
import com.interchangeplanner.shared.money.Money;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ExpenseCalculatorTest {

    @ParameterizedTest(name = "{0} em {1} meses = {2} ocorrências")
    @CsvSource({
            // mestrado de 2 anos (decisão do plano)
            "ONE_TIME, 24, 1", "PER_UNIT, 24, 1", "MONTHLY, 24, 24", "SEMIANNUAL, 24, 4", "ANNUAL, 24, 2",
            // períodos parciais contam como uma ocorrência
            "SEMIANNUAL, 18, 3", "ANNUAL, 18, 2", "ANNUAL, 12, 1", "MONTHLY, 1, 1", "SEMIANNUAL, 1, 1"})
    void sugereOcorrenciasPeloHorizonte(Recurrence recurrence, int horizon, int expected) {
        assertThat(ExpenseCalculator.suggestedOccurrences(recurrence, horizon)).isEqualTo(expected);
    }

    @Test
    void personalizadoNaoTemSugestaoEExigeValor() {
        assertThat(ExpenseCalculator.suggestedOccurrences(Recurrence.CUSTOM, 24)).isNull();
        assertThatThrownBy(() -> ExpenseCalculator.resolveOccurrences(Recurrence.CUSTOM, null, 24))
                .isInstanceOf(BusinessRuleException.class);
        assertThat(ExpenseCalculator.resolveOccurrences(Recurrence.CUSTOM, 7, 24)).isEqualTo(7);
    }

    @Test
    void valorInformadoPrevaleceSobreASugestao() {
        assertThat(ExpenseCalculator.resolveOccurrences(Recurrence.MONTHLY, null, 24)).isEqualTo(24);
        assertThat(ExpenseCalculator.resolveOccurrences(Recurrence.MONTHLY, 18, 24)).isEqualTo(18);
    }

    @Test
    void unicoEPorUnidadeTemSempreUmaOcorrencia() {
        assertThat(ExpenseCalculator.resolveOccurrences(Recurrence.PER_UNIT, 1, 24)).isEqualTo(1);
        assertThatThrownBy(() -> ExpenseCalculator.resolveOccurrences(Recurrence.ONE_TIME, 3, 24))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("quantidade");
    }

    @Test
    void totalEhPrecoVezesQuantidadeVezesOcorrencias() {
        // "Por UNI" da planilha: 5 inscrições de R$ 180
        assertThat(ExpenseCalculator.total(Money.of("180", "BRL"), 5, 1)).isEqualTo(Money.of("900", "BRL"));
        // aluguel mensal por 24 meses
        assertThat(ExpenseCalculator.total(Money.of("650", "EUR"), 1, 24)).isEqualTo(Money.of("15600", "EUR"));
    }

    @Test
    void statusDerivadoDosPagamentos() {
        Money total = Money.of("1000", "BRL");
        assertThat(ExpenseCalculator.status(false, total, Money.zero("BRL"))).isEqualTo(ExpenseStatus.PENDING);
        assertThat(ExpenseCalculator.status(false, total, Money.of("400", "BRL"))).isEqualTo(ExpenseStatus.PARTIALLY_PAID);
        assertThat(ExpenseCalculator.status(false, total, Money.of("1000", "BRL"))).isEqualTo(ExpenseStatus.PAID);
        // pagamento diferente do previsto (R$ 219,80 previsto, R$ 224 pago) continua "pago"
        assertThat(ExpenseCalculator.status(false, Money.of("219.80", "BRL"), Money.of("224", "BRL")))
                .isEqualTo(ExpenseStatus.PAID);
        assertThat(ExpenseCalculator.status(true, total, Money.of("1000", "BRL"))).isEqualTo(ExpenseStatus.CANCELLED);
    }

    @Test
    void gastoDeValorZeroSoFicaPagoComPagamento() {
        assertThat(ExpenseCalculator.status(false, Money.zero("BRL"), Money.zero("BRL"))).isEqualTo(ExpenseStatus.PENDING);
    }

    @Test
    void restanteNuncaFicaNegativo() {
        assertThat(ExpenseCalculator.remaining(Money.of("219.80", "BRL"), Money.of("224", "BRL")).isZero()).isTrue();
        assertThat(ExpenseCalculator.remaining(Money.of("1000", "BRL"), Money.of("400", "BRL")))
                .isEqualTo(Money.of("600", "BRL"));
    }
}
