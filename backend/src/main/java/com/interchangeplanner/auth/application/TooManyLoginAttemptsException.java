package com.interchangeplanner.auth.application;

import com.interchangeplanner.shared.error.ApiException;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

public class TooManyLoginAttemptsException extends ApiException {

    private final Duration retryAfter;

    public TooManyLoginAttemptsException(Duration retryAfter) {
        super(HttpStatus.TOO_MANY_REQUESTS, "Muitas tentativas",
                "Muitas tentativas de login. Tente novamente em " + minutesRoundedUp(retryAfter) + " minuto(s).",
                null);
        this.retryAfter = retryAfter;
    }

    public Duration getRetryAfter() {
        return retryAfter;
    }

    @Override
    public HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.RETRY_AFTER, Long.toString(Math.max(1, retryAfter.toSeconds())));
        return headers;
    }

    private static long minutesRoundedUp(Duration d) {
        return Math.max(1, (d.toSeconds() + 59) / 60);
    }
}
