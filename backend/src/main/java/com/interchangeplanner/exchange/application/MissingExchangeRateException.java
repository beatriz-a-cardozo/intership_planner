package com.interchangeplanner.exchange.application;

import com.interchangeplanner.shared.error.ApiException;
import org.springframework.http.HttpStatus;

public class MissingExchangeRateException extends ApiException {

    public MissingExchangeRateException(String from, String to) {
        super(HttpStatus.UNPROCESSABLE_CONTENT, "Cotação indisponível",
                "Não há cotação configurada para converter " + from + " em " + to + ".", null);
    }
}
