package com.interchangeplanner.exchange.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Cotação efetivamente usada num cálculo: 1 {@code base} = {@code rate} {@code quote}.
 *
 * @param source        provedor (FRANKFURTER, AWESOMEAPI), MANUAL (cotação do usuário) ou FALLBACK (reserva configurada)
 * @param effectiveDate data de referência no provedor (nula para MANUAL/FALLBACK)
 * @param fetchedAt     quando foi obtida/definida (nula para FALLBACK)
 * @param stale         true se a cotação está desatualizada (provedor falhando) ou é a reserva
 * @param manual        true se é a cotação manual do usuário
 */
public record ResolvedRate(String base, String quote, BigDecimal rate, String source, LocalDate effectiveDate,
        Instant fetchedAt, boolean stale, boolean manual) {

    public static final String SOURCE_MANUAL = "MANUAL";
    public static final String SOURCE_FALLBACK = "FALLBACK";
    static final int RATE_SCALE = 8;

    public ResolvedRate inverse() {
        BigDecimal inverted = BigDecimal.ONE.divide(rate, RATE_SCALE, RoundingMode.HALF_EVEN);
        return new ResolvedRate(quote, base, inverted, source, effectiveDate, fetchedAt, stale, manual);
    }

    public String pairKey() {
        return key(base, quote);
    }

    static String key(String base, String quote) {
        return base + ">" + quote;
    }
}
