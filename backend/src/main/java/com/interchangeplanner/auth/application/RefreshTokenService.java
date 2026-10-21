package com.interchangeplanner.auth.application;

import com.interchangeplanner.auth.domain.RefreshToken;
import com.interchangeplanner.auth.domain.RefreshTokenRepository;
import com.interchangeplanner.config.AppProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Refresh tokens opacos e rotativos: cada uso gera um novo token e revoga o anterior.
 * Reapresentar um token já revogado indica vazamento: a família inteira é revogada.
 */
@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    /** Token em claro (vai só para o cookie) + dono. */
    public record Issued(String rawToken, UUID userId, Duration ttl) {
    }

    private final RefreshTokenRepository repository;
    private final Clock clock;
    private final Duration ttl;

    public RefreshTokenService(RefreshTokenRepository repository, Clock clock, AppProperties properties) {
        this.repository = repository;
        this.clock = clock;
        this.ttl = properties.security().refreshToken().ttl();
    }

    /** Novo login: inicia uma família nova. */
    @Transactional
    public Issued issueForNewSession(UUID userId) {
        return issue(userId, UUID.randomUUID());
    }

    /**
     * Valida e rotaciona. Lança {@link InvalidRefreshTokenException} se inválido, expirado ou reutilizado.
     * A revogação da família em caso de reuso é gravada mesmo com a exceção (noRollbackFor).
     */
    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public Issued rotate(String rawToken) {
        RefreshToken current = find(rawToken);
        Instant now = clock.instant();
        if (current.isRevoked()) {
            int revoked = repository.revokeFamily(current.getFamilyId(), now);
            log.warn("Reuso de refresh token detectado; família {} revogada ({} token(s) ativo(s))",
                    current.getFamilyId(), revoked);
            throw new InvalidRefreshTokenException();
        }
        if (current.isExpired(now)) {
            throw new InvalidRefreshTokenException();
        }
        current.revoke(now);
        return issue(current.getUserId(), current.getFamilyId());
    }

    /** Logout: revoga a família do token apresentado (encerra esta sessão). Silencioso se o token não existir. */
    @Transactional
    public void revoke(String rawToken) {
        if (!isPlausible(rawToken)) {
            return;
        }
        repository.findByTokenHash(hash(rawToken))
                .ifPresent(t -> repository.revokeFamily(t.getFamilyId(), clock.instant()));
    }

    /** Remove tokens expirados há mais de um dia (mantém um pouco de histórico para detectar reuso). */
    @Transactional
    public int purgeExpired() {
        return repository.deleteExpiredBefore(clock.instant().minus(Duration.ofDays(1)));
    }

    private RefreshToken find(String rawToken) {
        if (!isPlausible(rawToken)) {
            throw new InvalidRefreshTokenException();
        }
        return repository.findByTokenHash(hash(rawToken)).orElseThrow(InvalidRefreshTokenException::new);
    }

    private static boolean isPlausible(String rawToken) {
        return rawToken != null && !rawToken.isBlank() && rawToken.length() <= 200;
    }

    private Issued issue(UUID userId, UUID familyId) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        repository.save(RefreshToken.issue(userId, hash(raw), familyId, clock.instant().plus(ttl)));
        return new Issued(raw, userId, ttl);
    }

    static String hash(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }
}
