package com.interchangeplanner.auth.api;

import com.interchangeplanner.user.api.MeResponse;

/**
 * Resposta de login/registro/refresh. O refresh token NÃO vem no corpo: vai num cookie HttpOnly.
 *
 * @param expiresIn validade do access token em segundos
 */
public record AuthResponse(String accessToken, String tokenType, long expiresIn, MeResponse user) {
}
