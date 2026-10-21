package com.interchangeplanner.user.domain;

import java.util.UUID;

/**
 * Publicado na mesma transação do cadastro. Módulos que precisam preparar dados padrão do usuário
 * (ex.: categorias e ciclos de gasto) escutam este evento, sem que o módulo de auth os conheça.
 */
public record UserRegisteredEvent(UUID userId) {
}
