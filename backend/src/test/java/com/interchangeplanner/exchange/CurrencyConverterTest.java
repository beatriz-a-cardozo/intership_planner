package com.interchangeplanner.exchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.interchangeplanner.exchange.application.CurrencyConverter;
import com.interchangeplanner.exchange.application.MissingExchangeRateException;
import com.interchangeplanner.exchange.application.ResolvedRate;
import com.interchangeplanner.shared.money.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class CurrencyConverterTest {

    static ResolvedRate eurBrl(String rate) {
        return new ResolvedRate("EUR", "BRL", new BigDecimal(rate), "FRANKFURTER", LocalDate.of(2026, 10, 21),
                Instant.parse("2026-10-21T08:00:00Z"), false, false);
    }

    private final CurrencyConverter converter = new CurrencyConverter(List.of(eurBrl("6.2")));

    @Test
    void mesmaMoedaNaoConverte() {
        Money brl = Money.of("100", "BRL");
        assertThat(converter.convert(brl, "BRL")).isSameAs(brl);
    }

    @Test
    void converteNaDirecaoConfigurada() {
        assertThat(converter.convert(Money.of("500", "EUR"), "BRL")).isEqualTo(Money.of("3100", "BRL"));
    }

    @Test
    void converteNaDirecaoInversa() {
        assertThat(converter.convert(Money.of("3100", "BRL"), "EUR")).isEqualTo(Money.of("500", "EUR"));
        assertThat(converter.convert(Money.of("100", "BRL"), "EUR").amount()).isEqualByComparingTo("16.1290");
    }

    @Test
    void parSemCotacaoEhErroExplicito() {
        assertThatThrownBy(() -> converter.convert(Money.of("1", "USD"), "BRL"))
                .isInstanceOf(MissingExchangeRateException.class);
    }

    @Test
    void fatorEInversoPreservamMetadados() {
        ResolvedRate inverse = converter.rate("BRL", "EUR").orElseThrow();
        assertThat(inverse.base()).isEqualTo("BRL");
        assertThat(inverse.source()).isEqualTo("FRANKFURTER");
        assertThat(converter.factor("EUR", "EUR")).isEqualByComparingTo("1");
    }
}
