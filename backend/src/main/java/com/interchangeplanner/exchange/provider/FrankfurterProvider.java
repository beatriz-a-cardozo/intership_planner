package com.interchangeplanner.exchange.provider;

import com.interchangeplanner.exchange.ExchangeProperties;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Frankfurter (dados de referência do BCE): sem chave, atualização diária em dias úteis, com histórico. */
@Component
public class FrankfurterProvider implements ExchangeRateProvider {

    public static final String ID = "frankfurter";
    static final String SOURCE = "FRANKFURTER";

    record Latest(String base, LocalDate date, Map<String, BigDecimal> rates) {
    }

    record Series(String base, Map<String, Map<String, BigDecimal>> rates) {
    }

    private final RestClient http;
    private final String baseUrl;

    @Autowired
    public FrankfurterProvider(@Qualifier("exchangeRestClient") RestClient http, ExchangeProperties properties) {
        this(http, properties.frankfurter().baseUrl());
    }

    FrankfurterProvider(RestClient http, String baseUrl) {
        this.http = http;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public FetchedRate latest(String base, String quote) {
        Latest body = get(baseUrl + "/latest?base={base}&symbols={quote}", Latest.class, base, quote);
        BigDecimal rate = body == null || body.rates() == null ? null : body.rates().get(quote);
        if (rate == null || rate.signum() <= 0 || body.date() == null) {
            throw new ExchangeRateProviderException("Frankfurter sem cotação " + base + "/" + quote);
        }
        return new FetchedRate(base, quote, rate, body.date(), SOURCE);
    }

    @Override
    public List<FetchedRate> history(String base, String quote, LocalDate from, LocalDate to) {
        Series body = get(baseUrl + "/{from}..{to}?base={base}&symbols={quote}", Series.class, from, to, base, quote);
        if (body == null || body.rates() == null) {
            return List.of();
        }
        List<FetchedRate> result = new ArrayList<>();
        body.rates().forEach((date, values) -> {
            BigDecimal rate = values.get(quote);
            if (rate != null && rate.signum() > 0) {
                result.add(new FetchedRate(base, quote, rate, LocalDate.parse(date), SOURCE));
            }
        });
        result.sort(Comparator.comparing(FetchedRate::effectiveDate));
        return result;
    }

    private <T> T get(String uri, Class<T> type, Object... vars) {
        try {
            return http.get().uri(uri, vars).retrieve().body(type);
        } catch (RestClientException e) {
            throw new ExchangeRateProviderException("Falha ao consultar Frankfurter", e);
        }
    }
}
