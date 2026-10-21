package com.interchangeplanner.shared.error;

import org.springframework.http.HttpStatus;

/** Regra de negócio violada com dados sintaticamente válidos (HTTP 422). */
public class BusinessRuleException extends ApiException {

    public BusinessRuleException(String detail, String field) {
        super(HttpStatus.UNPROCESSABLE_CONTENT, "Regra de negócio violada", detail, field);
    }

    public BusinessRuleException(String detail) {
        this(detail, null);
    }
}
