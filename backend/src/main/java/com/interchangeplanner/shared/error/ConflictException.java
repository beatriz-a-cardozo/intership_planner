package com.interchangeplanner.shared.error;

import org.springframework.http.HttpStatus;

public class ConflictException extends ApiException {

    public ConflictException(String detail, String field) {
        super(HttpStatus.CONFLICT, "Conflito", detail, field);
    }
}
