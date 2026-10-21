package com.interchangeplanner.exchange;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.interchangeplanner.exchange.application.ExchangeRateService;
import com.interchangeplanner.exchange.provider.FetchedRate;
import com.interchangeplanner.support.IntegrationTest;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ExchangeRatesIT extends IntegrationTest {

    @Autowired
    ExchangeRateService service;

    @Test
    void cotacaoManualPrevaleceEPodeSerRemovida() throws Exception {
        service.record(new FetchedRate("EUR", "BRL", new BigDecimal("5.6432"), LocalDate.now(), "FRANKFURTER"));
        TestSession bia = registerUser("fx");

        getAs(bia, "/api/exchange-rates/current")
                .andExpect(jsonPath("$.manual").value(false))
                .andExpect(jsonPath("$.stale").value(false))
                .andExpect(jsonPath("$.source").value("FRANKFURTER"));

        putAs(bia, "/api/exchange-rates/override", """
                {"base":"EUR","quote":"BRL","rate":6}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.manual").value(true))
                .andExpect(jsonPath("$.rate").value(6.0))
                .andExpect(jsonPath("$.marketRate").value(5.6432));

        // na direção inversa, a manual é invertida
        getAs(bia, "/api/exchange-rates/current?base=BRL&quote=EUR")
                .andExpect(jsonPath("$.manual").value(true))
                .andExpect(jsonPath("$.rate").value(0.16666667));

        deleteAs(bia, "/api/exchange-rates/override").andExpect(status().isNoContent());
        getAs(bia, "/api/exchange-rates/current").andExpect(jsonPath("$.manual").value(false));
    }

    @Test
    void historicoDiarioNaJanelaPedida() throws Exception {
        service.record(new FetchedRate("EUR", "BRL", new BigDecimal("5.80"), LocalDate.of(2020, 1, 2), "FRANKFURTER"));
        service.record(new FetchedRate("EUR", "BRL", new BigDecimal("5.85"), LocalDate.of(2020, 1, 2), "AWESOMEAPI"));
        service.record(new FetchedRate("EUR", "BRL", new BigDecimal("5.90"), LocalDate.of(2020, 1, 3), "FRANKFURTER"));
        TestSession bia = registerUser("fxhist");

        getAs(bia, "/api/exchange-rates/history?from=2020-01-01&to=2020-01-05")
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].date").value("2020-01-02"))
                .andExpect(jsonPath("$[0].rate").value(5.85))
                .andExpect(jsonPath("$[1].rate").value(5.9));
        // volta à cotação "atual" para não afetar outros testes
        service.record(new FetchedRate("EUR", "BRL", new BigDecimal("5.6432"), LocalDate.now(), "FRANKFURTER"));
    }

    @Test
    void validaACotacaoManual() throws Exception {
        TestSession bia = registerUser("fxval");
        putAs(bia, "/api/exchange-rates/override", """
                {"base":"EUR","quote":"BRL","rate":0}""").andExpect(status().isBadRequest());
        putAs(bia, "/api/exchange-rates/override", """
                {"base":"USD","quote":"BRL","rate":5}""").andExpect(status().isUnprocessableContent());
    }
}
