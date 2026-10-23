package com.interchangeplanner.finance.ledger.application;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Diário (e na inicialização): estende a janela das recorrências e confirma as automáticas vencidas. Idempotente. */
@Component
@ConditionalOnProperty(prefix = "app.jobs", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RecurringMaterializationJob {

    private static final Logger log = LoggerFactory.getLogger(RecurringMaterializationJob.class);

    private final RecurringService service;

    public RecurringMaterializationJob(RecurringService service) {
        this.service = service;
    }

    @SchedulerLock(name = "recurringMaterialization")

    @Scheduled(cron = "${app.finance.materialize-cron:0 15 3 * * *}", zone = "UTC")
    @EventListener(ApplicationReadyEvent.class)
    public void run() {
        int created = service.extendAndAutoConfirm();
        if (created > 0) {
            log.info("Ocorrências de recorrências geradas: {}", created);
        }
    }
}
