package com.interchangeplanner.config;

import com.interchangeplanner.auth.application.RefreshTokenCleanupJob;
import com.interchangeplanner.exchange.application.ExchangeRateRefreshJob;
import com.interchangeplanner.finance.ledger.application.RecurringMaterializationJob;
import com.interchangeplanner.notification.application.NotificationJob;
import com.interchangeplanner.shared.error.NotFoundException;
import io.swagger.v3.oas.annotations.Hidden;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dispara os jobs agendados por HTTP, para hospedagens gratuitas que "dormem" sem tráfego (o agendador interno não
 * roda com a API parada). Um cron do GitHub Actions chama este endpoint com o token de {@code app.internal.job-token}.
 * Sem token configurado, o endpoint não existe (404). Os jobs mantêm o ShedLock: chamar junto com o agendador interno
 * não executa duas vezes.
 */
@Hidden
@RestController
@RequestMapping("/api/internal/jobs")
public class InternalJobsController {

    private final String token;
    private final ObjectProvider<ExchangeRateRefreshJob> exchange;
    private final ObjectProvider<NotificationJob> notifications;
    private final ObjectProvider<RecurringMaterializationJob> recurring;
    private final ObjectProvider<RefreshTokenCleanupJob> tokens;

    public InternalJobsController(@Value("${app.internal.job-token:}") String token,
            ObjectProvider<ExchangeRateRefreshJob> exchange, ObjectProvider<NotificationJob> notifications,
            ObjectProvider<RecurringMaterializationJob> recurring, ObjectProvider<RefreshTokenCleanupJob> tokens) {
        this.token = token;
        this.exchange = exchange;
        this.notifications = notifications;
        this.recurring = recurring;
        this.tokens = tokens;
    }

    public record JobsResult(List<String> ran) {
    }

    /**
     * {@code exchange}: atualiza as cotações (e avalia os alertas). {@code daily}: recorrências, notificações e limpeza
     * de sessões expiradas.
     */
    @PostMapping("/{job}")
    public ResponseEntity<JobsResult> run(@PathVariable String job,
            @RequestHeader(name = "X-Job-Token", required = false) String provided) {
        if (token.isBlank()) {
            throw new NotFoundException("Recurso");
        }
        if (provided == null || !MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8),
                provided.getBytes(StandardCharsets.UTF_8))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<String> ran = new ArrayList<>();
        switch (job) {
            case "exchange" -> exchange.ifAvailable(j -> {
                j.refresh();
                ran.add("exchange");
            });
            case "daily" -> {
                recurring.ifAvailable(j -> {
                    j.run();
                    ran.add("recurring");
                });
                notifications.ifAvailable(j -> {
                    j.daily();
                    ran.add("notifications");
                });
                tokens.ifAvailable(j -> {
                    j.purge();
                    ran.add("refresh-tokens");
                });
            }
            default -> throw new NotFoundException("Job");
        }
        return ResponseEntity.ok(new JobsResult(ran));
    }
}
