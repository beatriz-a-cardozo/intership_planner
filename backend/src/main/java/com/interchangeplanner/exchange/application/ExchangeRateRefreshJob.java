package com.interchangeplanner.exchange.application;

import com.interchangeplanner.exchange.ExchangeProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Atualiza as cotações a cada 6h e, na inicialização, se estiverem ausentes ou desatualizadas. */
@Component
@ConditionalOnProperty(prefix = "app.jobs", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ExchangeRateRefreshJob {

    private static final Logger log = LoggerFactory.getLogger(ExchangeRateRefreshJob.class);

    private final ExchangeRateService service;
    private final ExchangeProperties properties;

    public ExchangeRateRefreshJob(ExchangeRateService service, ExchangeProperties properties) {
        this.service = service;
        this.properties = properties;
    }

    @Scheduled(cron = "${app.exchange.refresh-cron:0 0 */6 * * *}", zone = "UTC")
    public void refresh() {
        int updated = service.refreshAll();
        log.info("Cotações atualizadas: {}/{}", updated, properties.pairs().size());
    }

    /** Em thread virtual: a API sobe mesmo com o provedor lento ou fora do ar. */
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        if (!properties.refreshOnStartup()) {
            return;
        }
        Thread.ofVirtual().name("exchange-startup").start(() -> {
            try {
                int backfilled = service.backfillHistoryIfEmpty();
                if (backfilled > 0) {
                    log.info("Histórico de câmbio carregado: {} cotações", backfilled);
                }
                if (service.needsRefresh()) {
                    refresh();
                }
            } catch (RuntimeException e) {
                log.warn("Falha ao atualizar câmbio na inicialização: {}", e.getMessage());
            }
        });
    }
}
