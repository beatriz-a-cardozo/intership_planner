package com.interchangeplanner.exchange.provider;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.interchangeplanner.exchange.ExchangeProperties;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** AwesomeAPI (economia.awesomeapi.com.br): cotação intradiária com BRL, usada como secundária. */
@Component
public class AwesomeApiProvider implements ExchangeRateProvider {

    public static final String ID = "awesomeapi";
    static final String SOURCE = "AWESOMEAPI";
    private static final DateTimeFormatter CREATE_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    record Quote(String bid, @JsonProperty("create_date") String createDate) {
    }

    private final RestClient http;
    private final String baseUrl;

    @Autowired
    public AwesomeApiProvider(@Qualifier("exchangeRestClient") RestClient http, ExchangeProperties properties) {
        this(http, properties.awesomeapi().baseUrl());
    }

    AwesomeApiProvider(RestClient http, String baseUrl) {
        this.http = http;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public FetchedRate latest(String base, String quote) {
        Map<String, Quote> body;
        try {
            body = http.get().uri(baseUrl + "/last/{base}-{quote}", base, quote).retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Quote>>() {
                    });
        } catch (RestClientException e) {
            throw new ExchangeRateProviderException("Falha ao consultar AwesomeAPI", e);
        }
        Quote q = body == null ? null : body.get(base + quote);
        if (q == null || q.bid() == null) {
            throw new ExchangeRateProviderException("AwesomeAPI sem cotação " + base + "/" + quote);
        }
        try {
            BigDecimal rate = new BigDecimal(q.bid());
            if (rate.signum() <= 0) {
                throw new ExchangeRateProviderException("AwesomeAPI retornou cotação inválida");
            }
            return new FetchedRate(base, quote, rate, parseDate(q.createDate()), SOURCE);
        } catch (NumberFormatException e) {
            throw new ExchangeRateProviderException("AwesomeAPI retornou cotação inválida", e);
        }
    }

    private static LocalDate parseDate(String createDate) {
        try {
            return LocalDateTime.parse(createDate, CREATE_DATE).toLocalDate();
        } catch (DateTimeParseException | NullPointerException e) {
            throw new ExchangeRateProviderException("AwesomeAPI retornou data inválida", e);
        }
    }
}
