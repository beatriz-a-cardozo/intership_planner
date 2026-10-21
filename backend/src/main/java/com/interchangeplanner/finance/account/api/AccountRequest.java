package com.interchangeplanner.finance.account.api;

import com.interchangeplanner.finance.account.domain.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** A moeda é definida na criação e não pode mudar depois (o histórico de saldos está nela). */
public record AccountRequest(
        @NotBlank(message = "Informe o nome da conta.") @Size(max = 80, message = "Máximo de 80 caracteres.") String name,
        @NotNull(message = "Informe a moeda.") @Pattern(regexp = "[A-Z]{3}", message = "Moeda inválida.") String currency,
        @NotNull(message = "Informe o tipo da conta.") AccountType type,
        UUID objectiveId,
        Boolean archived) {
}
