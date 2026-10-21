package com.interchangeplanner.auth.application;

import com.interchangeplanner.shared.error.ApiException;
import org.springframework.http.HttpStatus;

public class InvalidRefreshTokenException extends ApiException {

    public InvalidRefreshTokenException() {
        super(HttpStatus.UNAUTHORIZED, "Sessão expirada", "Sua sessão expirou. Faça login novamente.", null);
    }
}
