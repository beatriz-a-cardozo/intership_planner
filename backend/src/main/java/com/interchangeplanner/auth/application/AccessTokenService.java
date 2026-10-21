package com.interchangeplanner.auth.application;

import com.interchangeplanner.config.AppProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/** Emite o access token (JWT curto). A validação fica com o resource server do Spring Security. */
@Service
public class AccessTokenService {

    public record IssuedToken(String value, Duration ttl) {
    }

    private final JwtEncoder encoder;
    private final Clock clock;
    private final String issuer;
    private final Duration ttl;

    public AccessTokenService(JwtEncoder encoder, Clock clock, AppProperties properties) {
        this.encoder = encoder;
        this.clock = clock;
        this.issuer = properties.security().jwt().issuer();
        this.ttl = properties.security().jwt().accessTokenTtl();
    }

    public IssuedToken issue(UUID userId) {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(userId.toString())
                .issuedAt(now)
                .expiresAt(now.plus(ttl))
                .id(UUID.randomUUID().toString())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        String value = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedToken(value, ttl);
    }
}
