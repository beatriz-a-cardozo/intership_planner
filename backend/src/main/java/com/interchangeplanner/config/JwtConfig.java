package com.interchangeplanner.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * JWT assinado com HMAC-SHA256 usando a infraestrutura padrão do Spring Security (Nimbus),
 * em vez de uma implementação manual. Trocar para RS256 (chaves assimétricas) no futuro
 * afeta só esta classe.
 */
@Configuration
public class JwtConfig {

    private final SecretKey key;
    private final String issuer;

    public JwtConfig(AppProperties properties) {
        byte[] secret = properties.security().jwt().secret().getBytes(StandardCharsets.UTF_8);
        this.key = new SecretKeySpec(secret, "HmacSHA256");
        this.issuer = properties.security().jwt().issuer();
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(key));
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(issuer)));
        return decoder;
    }
}
