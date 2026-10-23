package com.interchangeplanner.notification.application;

import com.interchangeplanner.exchange.application.ExchangeRatesRefreshedEvent;
import com.interchangeplanner.user.domain.AppUser;
import com.interchangeplanner.user.domain.UserRepository;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Gera as notificações de todas as pessoas uma vez por dia (08:00 de {@code app.notifications.zone}) e na
 * inicialização; avalia os alertas de câmbio a cada atualização de cotação. A deduplicação torna as execuções
 * repetidas inofensivas.
 */
@Component
@ConditionalOnProperty(prefix = "app.jobs", name = "enabled", havingValue = "true", matchIfMissing = true)
public class NotificationJob {

    private static final Logger log = LoggerFactory.getLogger(NotificationJob.class);

    private final UserRepository users;
    private final NotificationService notifications;
    private final ExchangeAlertService alerts;

    public NotificationJob(UserRepository users, NotificationService notifications, ExchangeAlertService alerts) {
        this.users = users;
        this.notifications = notifications;
        this.alerts = alerts;
    }

    @SchedulerLock(name = "notifications")
    @Scheduled(cron = "${app.notifications.cron:0 0 8 * * *}", zone = "${app.notifications.zone:America/Sao_Paulo}")
    public void daily() {
        int created = 0;
        for (AppUser user : users.findAll()) {
            try {
                created += notifications.generateFor(user.getId());
            } catch (RuntimeException e) {
                log.warn("Falha ao gerar notificações de {}: {}", user.getId(), e.getMessage());
            }
        }
        int fired = alerts.evaluateAll();
        log.info("Notificações geradas: {} (+{} alertas de câmbio)", created, fired);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        Thread.ofVirtual().name("notifications-startup").start(() -> {
            try {
                daily();
            } catch (RuntimeException e) {
                log.warn("Falha ao gerar notificações na inicialização: {}", e.getMessage());
            }
        });
    }

    @EventListener
    public void onRatesRefreshed(ExchangeRatesRefreshedEvent event) {
        if (event.updated() > 0) {
            alerts.evaluateAll();
        }
    }
}
