package com.interchangeplanner.auth.application;

import com.interchangeplanner.config.AppProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Proteção simples contra força bruta: após N falhas seguidas para o mesmo e-mail, bloqueia por um período.
 *
 * <p>Em memória (por instância). Suficiente para uma instância; com várias, trocar por um store
 * compartilhado sem mudar a interface pública.</p>
 */
@Component
public class LoginAttemptService {

    /** Limite de chaves monitoradas para não crescer sem controle sob ataque com e-mails aleatórios. */
    static final int MAX_TRACKED_KEYS = 10_000;

    private record Attempts(int failures, Instant lockedUntil, Instant lastFailure) {
    }

    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();
    private final Clock clock;
    private final int maxFailedAttempts;
    private final Duration lockDuration;

    public LoginAttemptService(AppProperties properties, Clock clock) {
        this.clock = clock;
        this.maxFailedAttempts = Math.max(1, properties.security().login().maxFailedAttempts());
        this.lockDuration = properties.security().login().lockDuration();
    }

    /** Lança {@link TooManyLoginAttemptsException} se a chave estiver bloqueada. */
    public void checkAllowed(String key) {
        Attempts a = attempts.get(key);
        Instant now = clock.instant();
        if (a != null && a.lockedUntil() != null && now.isBefore(a.lockedUntil())) {
            throw new TooManyLoginAttemptsException(Duration.between(now, a.lockedUntil()));
        }
    }

    public void recordFailure(String key) {
        Instant now = clock.instant();
        if (attempts.size() >= MAX_TRACKED_KEYS) {
            evictStale(now);
        }
        attempts.compute(key, (k, a) -> {
            // Depois que um bloqueio expira, a contagem recomeça.
            boolean lockExpired = a != null && a.lockedUntil() != null && !now.isBefore(a.lockedUntil());
            int failures = (a == null || lockExpired ? 0 : a.failures()) + 1;
            Instant lockedUntil = failures >= maxFailedAttempts ? now.plus(lockDuration) : null;
            return new Attempts(failures, lockedUntil, now);
        });
    }

    public void recordSuccess(String key) {
        attempts.remove(key);
    }

    private void evictStale(Instant now) {
        Instant cutoff = now.minus(lockDuration);
        attempts.entrySet().removeIf(e -> e.getValue().lastFailure().isBefore(cutoff));
    }
}
