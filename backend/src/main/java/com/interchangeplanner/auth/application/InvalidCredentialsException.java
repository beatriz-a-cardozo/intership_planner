package com.interchangeplanner.auth.application;

import com.interchangeplanner.shared.error.ApiException;
import org.springframework.http.HttpStatus;

/** Mensagem única para e-mail inexistente e senha errada: não revela quais e-mails têm conta. */
public class InvalidCredentialsException extends ApiException {

    public InvalidCredentialsException() {
        super(HttpStatus.UNAUTHORIZED, "Credenciais inválidas", "E-mail ou senha incorretos.", null);
    }
}
