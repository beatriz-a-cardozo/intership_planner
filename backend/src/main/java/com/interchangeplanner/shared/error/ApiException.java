package com.interchangeplanner.shared.error;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

/** Exceção de negócio convertida em Problem Details pelo {@link GlobalExceptionHandler}. */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String title;
    private final String field;

    protected ApiException(HttpStatus status, String title, String detail, String field) {
        super(detail);
        this.status = status;
        this.title = title;
        this.field = field;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getTitle() {
        return title;
    }

    /** Campo do formulário associado ao erro (opcional). */
    public String getField() {
        return field;
    }

    /** Cabeçalhos extras da resposta de erro (ex.: Retry-After). */
    public HttpHeaders getHeaders() {
        return HttpHeaders.EMPTY;
    }
}
