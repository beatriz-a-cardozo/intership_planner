package com.interchangeplanner.exchange.provider;

import com.interchangeplanner.exchange.ExchangeProperties;
import java.net.http.HttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** Cliente HTTP dos provedores de câmbio, com timeouts curtos: um provedor lento não segura o job. */
@Configuration
public class ExchangeHttpConfig {

    public static final String USER_AGENT = "InterchangePlanner/1.0 (+exchange-rates)";

    @Bean
    public RestClient exchangeRestClient(ExchangeProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.httpTimeout())
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(properties.httpTimeout());
        return RestClient.builder()
                .requestFactory(factory)
                .defaultHeader("User-Agent", USER_AGENT)
                .build();
    }
}
