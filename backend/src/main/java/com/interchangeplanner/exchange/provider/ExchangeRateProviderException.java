package com.interchangeplanner.exchange.provider;

/** Falha ao consultar um provedor (rede, formato, par inexistente). Tratada com fallback, nunca exposta. */
public class ExchangeRateProviderException extends RuntimeException {

    public ExchangeRateProviderException(String message) {
        super(message);
    }

    public ExchangeRateProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
