package com.interchangeplanner.expense.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Categoria ou ciclo de gasto. */
public record CatalogRequest(
        @NotBlank(message = "Informe o nome.") @Size(max = 60, message = "Máximo de 60 caracteres.") String name,
        @NotBlank(message = "Escolha uma cor.") @Pattern(regexp = "#[0-9a-fA-F]{6}", message = "Use uma cor no formato #RRGGBB.")
        String color,
        @NotBlank(message = "Escolha um ícone.") @Pattern(regexp = "[a-z0-9-]{1,40}", message = "Ícone inválido.") String icon,
        Integer sortOrder,
        Boolean archived) {
}
