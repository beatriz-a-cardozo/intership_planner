package com.interchangeplanner.exchange.provider;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FetchedRate(String base, String quote, BigDecimal rate, LocalDate effectiveDate, String source) {
}
