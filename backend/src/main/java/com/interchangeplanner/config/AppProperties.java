package com.interchangeplanner.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Configurações da aplicação ({@code app.*}), validadas na inicialização. */
@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @NotBlank String name,
        @Valid @NotNull Security security,
        @Valid @NotNull Cors cors) {

    public record Security(@Valid @NotNull Jwt jwt, @Valid @NotNull RefreshToken refreshToken, @Valid @NotNull Login login) {
    }

    /**
     * @param secret chave HMAC do JWT; mínimo de 32 bytes (256 bits) para HS256.
     */
    public record Jwt(
            @NotBlank @Size(min = 32, message = "app.security.jwt.secret precisa ter pelo menos 32 caracteres") String secret,
            @NotBlank String issuer,
            @NotNull Duration accessTokenTtl) {
    }

    public record RefreshToken(
            @NotNull Duration ttl,
            @NotBlank String cookieName,
            @NotBlank String cookiePath,
            boolean cookieSecure,
            @Pattern(regexp = "Strict|Lax|None") String cookieSameSite) {
    }

    /** Proteção contra força bruta no login. */
    public record Login(int maxFailedAttempts, @NotNull Duration lockDuration) {
    }

    public record Cors(List<String> allowedOrigins) {
        public Cors {
            allowedOrigins = allowedOrigins == null ? List.of()
                    : allowedOrigins.stream().map(String::strip).filter(o -> !o.isEmpty()).toList();
        }
    }
}
