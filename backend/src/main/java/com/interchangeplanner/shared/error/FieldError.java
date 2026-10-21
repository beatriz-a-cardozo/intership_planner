package com.interchangeplanner.shared.error;

/** Erro associado a um campo da requisição, exposto em {@code errors[]} do Problem Details. */
public record FieldError(String field, String message) {
}
