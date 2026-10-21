package com.interchangeplanner.shared.security;

import java.util.UUID;

/**
 * Usuário autenticado da requisição, extraído do JWT (claim {@code sub}).
 * Declare como parâmetro de um método de controller para recebê-lo.
 * Services recebem SEMPRE o id daqui — nunca um userId vindo do corpo/URL.
 */
public record CurrentUser(UUID id) {
}
