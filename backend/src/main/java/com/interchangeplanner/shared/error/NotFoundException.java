package com.interchangeplanner.shared.error;

import org.springframework.http.HttpStatus;

/**
 * Recurso inexistente OU pertencente a outro usuário. As duas situações retornam 404
 * de propósito, para não revelar a existência de dados de terceiros.
 */
public class NotFoundException extends ApiException {

    public NotFoundException(String resource) {
        super(HttpStatus.NOT_FOUND, "Não encontrado", resource + " não encontrado(a).", null);
    }
}
