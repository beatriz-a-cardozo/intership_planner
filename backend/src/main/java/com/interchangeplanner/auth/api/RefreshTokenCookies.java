package com.interchangeplanner.auth.api;

import com.interchangeplanner.config.AppProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

/**
 * Cookie do refresh token: HttpOnly (JS não lê), Secure em produção, SameSite=Strict e path restrito a
 * /api/auth — o navegador só o envia para refresh/logout, nunca para as demais rotas.
 */
@Component
public class RefreshTokenCookies {

    private final AppProperties.RefreshToken config;

    public RefreshTokenCookies(AppProperties properties) {
        this.config = properties.security().refreshToken();
    }

    public String read(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, config.cookieName());
        return cookie == null ? null : cookie.getValue();
    }

    public String create(String rawToken, Duration ttl) {
        return base(rawToken).maxAge(ttl).build().toString();
    }

    public String clear() {
        return base("").maxAge(0).build().toString();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(config.cookieName(), value)
                .httpOnly(true)
                .secure(config.cookieSecure())
                .sameSite(config.cookieSameSite())
                .path(config.cookiePath());
    }
}
