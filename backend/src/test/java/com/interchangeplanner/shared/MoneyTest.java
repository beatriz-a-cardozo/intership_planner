package com.interchangeplanner.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.interchangeplanner.shared.money.CurrencyMismatchException;
import com.interchangeplanner.shared.money.Money;
import com.interchangeplanner.shared.money.MoneyResponse;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void somaNaMesmaMoeda() {
        assertThat(Money.of("10.10", "BRL").plus(Money.of("0.20", "BRL"))).isEqualTo(Money.of("10.30", "BRL"));
    }

    @Test
    void recusaSomarMoedasDiferentesSemConversao() {
        assertThatThrownBy(() -> Money.of("1", "BRL").plus(Money.of("1", "EUR")))
                .isInstanceOf(CurrencyMismatchException.class);
    }

    @Test
    void escalaInternaDe4CasasComHalfEven() {
        assertThat(Money.of("0.00005", "BRL").amount()).isEqualByComparingTo("0.0000");
        assertThat(Money.of("0.00015", "BRL").amount()).isEqualByComparingTo("0.0002");
    }

    @Test
    void apresentacaoArredondaPara2Casas() {
        assertThat(MoneyResponse.of(Money.of("1234.5650", "EUR")).amount()).isEqualTo(new BigDecimal("1234.56"));
        assertThat(MoneyResponse.of(Money.of("1234.5750", "EUR")).amount()).isEqualTo(new BigDecimal("1234.58"));
    }

    @Test
    void atLeastZeroEDivisao() {
        assertThat(Money.of("-5", "BRL").atLeastZero().isZero()).isTrue();
        assertThat(Money.of("100", "BRL").dividedBy(3).amount()).isEqualByComparingTo("33.3333");
    }

    @Test
    void recusaCodigoDeMoedaInvalido() {
        assertThatThrownBy(() -> Money.of("1", "real")).isInstanceOf(IllegalArgumentException.class);
    }
}
