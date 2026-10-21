package com.interchangeplanner.exchange.api;

import com.interchangeplanner.exchange.application.CurrencyConverter;
import com.interchangeplanner.exchange.application.ResolvedRate;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Cotação usada nos cálculos, com os dados para o selo de frescor no frontend
 * ("≈ R$ 3.100 · cotação de 21/10 08:00").
 *
 * @param marketRate cotação de mercado, para comparar quando a manual está ativa (nula em resumos de cálculo)
 */
public record ExchangeRateResponse(
        String base,
        String quote,
        BigDecimal rate,
        String source,
        @Schema(types = {"string", "null"}, format = "date") LocalDate effectiveDate,
        @Schema(types = {"string", "null"}, format = "date-time") Instant fetchedAt,
        boolean stale,
        boolean manual,
        @Schema(types = {"number", "null"}) BigDecimal marketRate) {

    public static ExchangeRateResponse of(ResolvedRate rate, ResolvedRate market) {
        return new ExchangeRateResponse(rate.base(), rate.quote(), rate.rate(), rate.source(), rate.effectiveDate(),
                rate.fetchedAt(), rate.stale(), rate.manual(), market.rate());
    }

    /** Cotação usada num cálculo (dashboard, resumos). */
    public static ExchangeRateResponse of(ResolvedRate rate) {
        return new ExchangeRateResponse(rate.base(), rate.quote(), rate.rate(), rate.source(), rate.effectiveDate(),
                rate.fetchedAt(), rate.stale(), rate.manual(), null);
    }

    public static List<ExchangeRateResponse> of(CurrencyConverter converter) {
        return converter.rates().stream().map(ExchangeRateResponse::of).toList();
    }
}
