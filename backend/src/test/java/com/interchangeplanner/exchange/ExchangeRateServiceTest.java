package com.interchangeplanner.exchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.interchangeplanner.exchange.application.ExchangeRateService;
import com.interchangeplanner.exchange.application.ResolvedRate;
import com.interchangeplanner.exchange.domain.ExchangeRate;
import com.interchangeplanner.exchange.domain.ExchangeRateRepository;
import com.interchangeplanner.exchange.domain.UserExchangeRateOverride;
import com.interchangeplanner.exchange.domain.UserExchangeRateOverrideRepository;
import com.interchangeplanner.exchange.provider.ExchangeRateProvider;
import com.interchangeplanner.exchange.provider.ExchangeRateProviderException;
import com.interchangeplanner.exchange.provider.FetchedRate;
import com.interchangeplanner.shared.error.BusinessRuleException;
import com.interchangeplanner.shared.money.Money;
import com.interchangeplanner.support.MutableClock;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExchangeRateServiceTest {

    static final UUID USER = UUID.randomUUID();

    /** Provedor de teste: responde com a taxa configurada ou falha. */
    static final class FakeProvider implements ExchangeRateProvider {
        final String id;
        BigDecimal rate;
        int calls;

        FakeProvider(String id, String rate) {
            this.id = id;
            this.rate = rate == null ? null : new BigDecimal(rate);
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public FetchedRate latest(String base, String quote) {
            calls++;
            if (rate == null) {
                throw new ExchangeRateProviderException(id + " fora do ar");
            }
            return new FetchedRate(base, quote, rate, LocalDate.of(2026, 10, 21), id.toUpperCase());
        }
    }

    MutableClock clock;
    ExchangeRateRepository rates;
    UserExchangeRateOverrideRepository overrides;
    List<ExchangeRate> stored;
    FakeProvider primary;
    FakeProvider secondary;
    ExchangeRateService service;

    static ExchangeProperties properties(List<String> providers) {
        return new ExchangeProperties(providers, Duration.ofHours(12), Duration.ofSeconds(5), false, 0,
                List.of(new ExchangeProperties.Pair("EUR", "BRL", new BigDecimal("6.0"))),
                new ExchangeProperties.Endpoint("http://frankfurter"), new ExchangeProperties.Endpoint("http://awesome"));
    }

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-10-21T12:00:00Z"));
        rates = mock(ExchangeRateRepository.class);
        overrides = mock(UserExchangeRateOverrideRepository.class);
        stored = new ArrayList<>();
        when(rates.save(any(ExchangeRate.class))).thenAnswer(inv -> {
            ExchangeRate r = inv.getArgument(0);
            stored.add(r);
            return r;
        });
        when(rates.findFirstByBaseAndQuoteOrderByFetchedAtDesc("EUR", "BRL"))
                .thenAnswer(inv -> stored.isEmpty() ? Optional.empty() : Optional.of(stored.getLast()));
        when(overrides.findByUserIdAndBaseAndQuote(any(), any(), any())).thenReturn(Optional.empty());
        primary = new FakeProvider("frankfurter", "6.10");
        secondary = new FakeProvider("awesomeapi", "6.15");
        service = new ExchangeRateService(rates, overrides, List.of(secondary, primary),
                properties(List.of("frankfurter", "awesomeapi")), clock);
    }

    @Test
    void usaOsProvedoresNaOrdemConfigurada() {
        assertThat(service.refreshAll()).isEqualTo(1);
        assertThat(primary.calls).isEqualTo(1);
        assertThat(secondary.calls).isZero();
        assertThat(service.market("EUR", "BRL").rate()).isEqualByComparingTo("6.10");
        assertThat(service.market("EUR", "BRL").source()).isEqualTo("FRANKFURTER");
    }

    @Test
    void falhaDoPrincipalCaiNoSecundario() {
        primary.rate = null;
        service.refreshAll();
        assertThat(secondary.calls).isEqualTo(1);
        assertThat(service.market("EUR", "BRL").source()).isEqualTo("AWESOMEAPI");
    }

    @Test
    void todosFalhandoMantemAUltimaValidaEMarcaComoDesatualizadaDepoisDoPrazo() {
        service.refreshAll();
        primary.rate = null;
        secondary.rate = null;

        clock.advance(Duration.ofHours(6));
        assertThat(service.refreshAll()).isZero();
        ResolvedRate sixHours = service.market("EUR", "BRL");
        assertThat(sixHours.rate()).isEqualByComparingTo("6.10");
        assertThat(sixHours.stale()).isFalse();

        clock.advance(Duration.ofHours(7));
        ResolvedRate thirteenHours = service.market("EUR", "BRL");
        assertThat(thirteenHours.rate()).isEqualByComparingTo("6.10");
        assertThat(thirteenHours.stale()).isTrue();
    }

    @Test
    void semNenhumaCotacaoUsaAReservaConfigurada() {
        ResolvedRate fallback = service.market("EUR", "BRL");
        assertThat(fallback.rate()).isEqualByComparingTo("6.0");
        assertThat(fallback.source()).isEqualTo(ResolvedRate.SOURCE_FALLBACK);
        assertThat(fallback.stale()).isTrue();
    }

    @Test
    void cotacaoManualPrevaleceInclusiveNaDirecaoInversa() {
        service.refreshAll();
        UserExchangeRateOverride manual = new UserExchangeRateOverride(USER, "EUR", "BRL", new BigDecimal("5.00"));
        when(overrides.findByUserIdAndBaseAndQuote(USER, "EUR", "BRL")).thenReturn(Optional.of(manual));

        assertThat(service.resolve(USER, "EUR", "BRL").manual()).isTrue();
        assertThat(service.resolve(USER, "BRL", "EUR").rate()).isEqualByComparingTo("0.2");
        assertThat(service.converterFor(USER).convert(Money.of("100", "EUR"), "BRL")).isEqualTo(Money.of("500", "BRL"));
        // outro usuário continua com a de mercado
        assertThat(service.resolve(UUID.randomUUID(), "EUR", "BRL").rate()).isEqualByComparingTo("6.10");
    }

    @Test
    void cotacaoManualDesativadaVoltaAoMercado() {
        service.refreshAll();
        UserExchangeRateOverride manual = new UserExchangeRateOverride(USER, "EUR", "BRL", new BigDecimal("5.00"));
        manual.deactivate();
        when(overrides.findByUserIdAndBaseAndQuote(USER, "EUR", "BRL")).thenReturn(Optional.of(manual));
        assertThat(service.resolve(USER, "EUR", "BRL").manual()).isFalse();
    }

    @Test
    void parNaoConfiguradoEhRecusado() {
        assertThatThrownBy(() -> service.market("USD", "BRL")).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void provedorDesconhecidoNaConfiguracaoFalhaNaInicializacao() {
        assertThatThrownBy(() -> new ExchangeRateService(rates, overrides, List.of(primary),
                properties(List.of("inexistente")), clock)).isInstanceOf(IllegalStateException.class);
    }
}
