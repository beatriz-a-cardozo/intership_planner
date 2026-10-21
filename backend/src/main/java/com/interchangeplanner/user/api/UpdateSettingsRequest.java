package com.interchangeplanner.user.api;

import com.interchangeplanner.user.domain.ThemePreference;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateSettingsRequest(
        @NotNull(message = "Informe o tema.") ThemePreference theme,
        @NotBlank(message = "Informe o idioma.") @Size(max = 10) String locale,
        @NotBlank(message = "Informe o fuso horário.") @Size(max = 60) String timezone,
        @NotNull(message = "Informe a moeda principal.") @Pattern(regexp = "[A-Z]{3}", message = "Moeda inválida.") String primaryCurrency,
        @NotNull(message = "Informe a moeda secundária.") @Pattern(regexp = "[A-Z]{3}", message = "Moeda inválida.") String secondaryCurrency) {
}
