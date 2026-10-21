package com.interchangeplanner.currency;

public record CurrencyResponse(String code, String name, String symbol, int scale) {

    static CurrencyResponse from(Currency c) {
        return new CurrencyResponse(c.getCode(), c.getName(), c.getSymbol(), c.getScale());
    }
}
