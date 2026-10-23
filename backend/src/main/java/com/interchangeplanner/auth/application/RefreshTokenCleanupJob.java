package com.interchangeplanner.auth.application;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Limpeza diária de refresh tokens expirados. Idempotente: seguro mesmo com várias instâncias. */
@Component
@ConditionalOnProperty(prefix = "app.jobs", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RefreshTokenCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenCleanupJob.class);

    private final RefreshTokenService refreshTokens;

    public RefreshTokenCleanupJob(RefreshTokenService refreshTokens) {
        this.refreshTokens = refreshTokens;
    }

    @SchedulerLock(name = "refreshTokenCleanup")

    @Scheduled(cron = "${app.jobs.refresh-token-cleanup-cron:0 30 3 * * *}", zone = "UTC")
    public void purge() {
        int removed = refreshTokens.purgeExpired();
        if (removed > 0) {
            log.info("Refresh tokens expirados removidos: {}", removed);
        }
    }
}
