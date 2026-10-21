package com.interchangeplanner.auth.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Informe seu nome.")
        @Size(min = 2, max = 80, message = "O nome deve ter entre 2 e 80 caracteres.")
        String displayName,

        @NotBlank(message = "Informe um e-mail válido.")
        @Email(message = "Informe um e-mail válido.")
        @Size(max = 254, message = "Máximo de 254 caracteres.")
        String email,

        @NotBlank(message = "Informe uma senha.")
        @Size(min = 8, max = 128, message = "A senha deve ter entre 8 e 128 caracteres.")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*[0-9]).+$", message = "A senha deve ter letras e números.")
        String password) {

    /** Apara espaços antes da validação (comum ao digitar no celular). A senha é mantida como veio. */
    public RegisterRequest {
        displayName = displayName == null ? null : displayName.strip();
        email = email == null ? null : email.strip();
    }

    @Override
    public String toString() {
        return "RegisterRequest[displayName=%s, email=%s, password=***]".formatted(displayName, email);
    }
}
