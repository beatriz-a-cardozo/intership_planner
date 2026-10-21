package com.interchangeplanner.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.interchangeplanner.auth.application.AccessTokenService;
import com.interchangeplanner.config.AppProperties;
import com.interchangeplanner.config.JwtConfig;
import com.interchangeplanner.support.MutableClock;
import com.interchangeplanner.support.TestProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;

class AccessTokenServiceTest {

    private final AppProperties properties = TestProperties.defaults();
    private final JwtConfig jwtConfig = new JwtConfig(properties);
    private final JwtDecoder decoder = jwtConfig.jwtDecoder();

    @Test
    void emiteJwtComSubjectEmissorEValidadeDe15Minutos() {
        UUID userId = UUID.randomUUID();
        AccessTokenService service = new AccessTokenService(jwtConfig.jwtEncoder(), java.time.Clock.systemUTC(),
                properties);

        AccessTokenService.IssuedToken token = service.issue(userId);
        Jwt jwt = decoder.decode(token.value());

        assertThat(jwt.getSubject()).isEqualTo(userId.toString());
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("interchange-planner");
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofMinutes(15));
        assertThat(token.ttl()).isEqualTo(Duration.ofMinutes(15));
    }

    @Test
    void tokenExpiradoEhRejeitado() {
        // Emitido há 1 hora: já passou da validade (15 min) e da tolerância padrão de relógio (60 s)
        MutableClock past = new MutableClock(Instant.now().minus(Duration.ofHours(1)));
        AccessTokenService service = new AccessTokenService(jwtConfig.jwtEncoder(), past, properties);
        String token = service.issue(UUID.randomUUID()).value();

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtValidationException.class);
    }

    @Test
    void tokenAssinadoComOutraChaveEhRejeitado() {
        AppProperties other = new AppProperties(properties.name(),
                new AppProperties.Security(
                        new AppProperties.Jwt("another-secret-with-at-least-32-chars!!", "interchange-planner",
                                Duration.ofMinutes(15)),
                        properties.security().refreshToken(), properties.security().login()),
                properties.cors());
        AccessTokenService foreign = new AccessTokenService(new JwtConfig(other).jwtEncoder(),
                java.time.Clock.systemUTC(), other);
        String token = foreign.issue(UUID.randomUUID()).value();

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(Exception.class);
    }
}
