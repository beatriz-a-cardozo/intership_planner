package com.interchangeplanner.exchange.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** Formato real das respostas (capturado em 21/10/2026), sem acessar a rede. */
class ExchangeRateProvidersTest {

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

    @Test
    void frankfurterLatest() {
        server.expect(requestTo("https://api.test/v1/latest?base=EUR&symbols=BRL")).andRespond(withSuccess(
                "{\"amount\":1.0,\"base\":\"EUR\",\"date\":\"2026-10-21\",\"rates\":{\"BRL\":5.6432}}",
                MediaType.APPLICATION_JSON));
        FetchedRate rate = new FrankfurterProvider(builder.build(), "https://api.test/v1/").latest("EUR", "BRL");
        assertThat(rate.rate()).isEqualByComparingTo("5.6432");
        assertThat(rate.effectiveDate()).isEqualTo(LocalDate.of(2026, 10, 21));
        assertThat(rate.source()).isEqualTo("FRANKFURTER");
    }

    @Test
    void frankfurterHistoricoOrdenadoPorData() {
        server.expect(requestTo("https://api.test/v1/2026-10-01..2026-10-07?base=EUR&symbols=BRL")).andRespond(withSuccess(
                "{\"amount\":1.0,\"base\":\"EUR\",\"start_date\":\"2026-10-01\",\"end_date\":\"2026-10-07\","
                        + "\"rates\":{\"2026-10-02\":{\"BRL\":5.861},\"2026-10-01\":{\"BRL\":5.862},\"2026-10-05\":{\"BRL\":5.5849}}}",
                MediaType.APPLICATION_JSON));
        List<FetchedRate> series = new FrankfurterProvider(builder.build(), "https://api.test/v1")
                .history("EUR", "BRL", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 7));
        assertThat(series).extracting(FetchedRate::effectiveDate).containsExactly(
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 2), LocalDate.of(2026, 10, 5));
    }

    @Test
    void awesomeApiLatest() {
        server.expect(requestTo("https://aw.test/json/last/EUR-BRL")).andRespond(withSuccess(
                "{\"EURBRL\":{\"code\":\"EUR\",\"codein\":\"BRL\",\"bid\":\"5.6465\",\"ask\":\"5.6485\","
                        + "\"timestamp\":\"1792599867\",\"create_date\":\"2026-10-21 13:24:27\"}}",
                MediaType.APPLICATION_JSON));
        FetchedRate rate = new AwesomeApiProvider(builder.build(), "https://aw.test/json").latest("EUR", "BRL");
        assertThat(rate.rate()).isEqualByComparingTo("5.6465");
        assertThat(rate.effectiveDate()).isEqualTo(LocalDate.of(2026, 10, 21));
    }

    @Test
    void erroHttpViraExcecaoDoProvedor() {
        server.expect(requestTo("https://api.test/v1/latest?base=EUR&symbols=BRL")).andRespond(withServerError());
        assertThatThrownBy(() -> new FrankfurterProvider(builder.build(), "https://api.test/v1").latest("EUR", "BRL"))
                .isInstanceOf(ExchangeRateProviderException.class);
    }

    @Test
    void respostaSemOParVirExcecaoDoProvedor() {
        server.expect(requestTo("https://aw.test/json/last/EUR-BRL"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> new AwesomeApiProvider(builder.build(), "https://aw.test/json").latest("EUR", "BRL"))
                .isInstanceOf(ExchangeRateProviderException.class);
    }
}
