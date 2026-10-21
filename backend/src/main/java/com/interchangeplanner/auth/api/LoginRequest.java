package com.interchangeplanner.auth.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "Informe seu e-mail.") @Size(max = 254) String email,
        // Sem regras de formato aqui: a senha é só comparada com o hash.
        @NotBlank(message = "Informe sua senha.") @Size(max = 128) String password) {

    public LoginRequest {
        email = email == null ? null : email.strip();
    }

    @Override
    public String toString() {
        return "LoginRequest[email=%s, password=***]".formatted(email);
    }
}
