package com.interchangeplanner.notification.application;

import com.interchangeplanner.exchange.application.ExchangeRateService;
import com.interchangeplanner.exchange.application.ResolvedRate;
import com.interchangeplanner.notification.api.NotificationDtos.AlertRequest;
import com.interchangeplanner.notification.api.NotificationDtos.AlertResponse;
import com.interchangeplanner.notification.application.NotificationDetector.Draft;
import com.interchangeplanner.notification.domain.ExchangeRateAlert;
import com.interchangeplanner.notification.domain.ExchangeRateAlertRepository;
import com.interchangeplanner.notification.domain.Notification.Severity;
import com.interchangeplanner.notification.domain.NotificationCategory;
import com.interchangeplanner.shared.error.BusinessRuleException;
import com.interchangeplanner.shared.error.NotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Alertas de cotação. Avaliados com a cotação de MERCADO (não a manual) depois de cada atualização de câmbio e no job
 * diário; a histerese fica no {@link ExchangeAlertEvaluator}.
 */
@Service
public class ExchangeAlertService {

    static final int MAX_PER_USER = 10;

    private final ExchangeRateAlertRepository alerts;
    private final ExchangeRateService exchange;
    private final NotificationService notifications;
    private final NotificationPreferenceService preferences;
    private final Clock clock;

    public ExchangeAlertService(ExchangeRateAlertRepository alerts, ExchangeRateService exchange,
            NotificationService notifications, NotificationPreferenceService preferences, Clock clock) {
        this.alerts = alerts;
        this.exchange = exchange;
        this.notifications = notifications;
        this.preferences = preferences;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<AlertResponse> list(UUID userId) {
        return alerts.findByUserIdOrderByCreatedAtAsc(userId).stream().map(ExchangeAlertService::toResponse).toList();
    }

    @Transactional
    public AlertResponse create(UUID userId, AlertRequest r) {
        validate(r);
        if (alerts.countByUserId(userId) >= MAX_PER_USER) {
            throw new BusinessRuleException("No máximo " + MAX_PER_USER + " alertas de câmbio.");
        }
        ExchangeRateAlert a = alerts.save(new ExchangeRateAlert(userId, r.base(), r.quote(), r.condition(), r.threshold()));
        evaluate(a);
        return toResponse(a);
    }

    @Transactional
    public AlertResponse update(UUID userId, UUID id, AlertRequest r) {
        validate(r);
        ExchangeRateAlert a = require(userId, id);
        a.update(r.base(), r.quote(), r.condition(), r.threshold(), r.active() == null || r.active());
        evaluate(a);
        return toResponse(a);
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        alerts.delete(require(userId, id));
    }

    /** Avalia todos os alertas ativos (uma cotação por par). */
    @Transactional
    public int evaluateAll() {
        Map<String, BigDecimal> rateCache = new HashMap<>();
        int fired = 0;
        for (ExchangeRateAlert a : alerts.findByActiveTrue()) {
            fired += evaluate(a, rateCache);
        }
        return fired;
    }

    private int evaluate(ExchangeRateAlert a) {
        return evaluate(a, new HashMap<>());
    }

    private int evaluate(ExchangeRateAlert a, Map<String, BigDecimal> rateCache) {
        if (!a.isActive()) {
            return 0;
        }
        BigDecimal rate = rateCache.computeIfAbsent(a.getBase() + a.getQuote(),
                k -> marketRate(a.getBase(), a.getQuote()));
        if (rate == null) {
            return 0;
        }
        var outcome = ExchangeAlertEvaluator.evaluate(a.getCondition(), a.getThreshold(), a.getState(), rate);
        a.apply(outcome.state(), rate, outcome.fire(), clock.instant());
        if (!outcome.fire() || !preferences.effective(a.getUserId()).get(NotificationCategory.EXCHANGE).enabled()) {
            return 0;
        }
        String verb = a.getCondition() == ExchangeRateAlert.Condition.BELOW ? "abaixo de" : "acima de";
        String title = a.getBase() + " " + verb + " " + fmt(a.getThreshold()) + " " + a.getQuote();
        String body = "Cotação agora: 1 " + a.getBase() + " = " + fmt(rate) + " " + a.getQuote() + ".";
        return notifications.insert(a.getUserId(), new Draft(NotificationCategory.EXCHANGE, Severity.WARNING, title, body,
                "/settings", "EXCHANGE_ALERT", a.getId(), "exchange:" + a.getId() + ":" + a.getTriggerCount()));
    }

    private static String fmt(BigDecimal v) {
        return v.setScale(4, RoundingMode.HALF_EVEN).stripTrailingZeros().toPlainString().replace('.', ',');
    }

    private void validate(AlertRequest r) {
        if (r.base().equals(r.quote())) {
            throw new BusinessRuleException("Escolha duas moedas diferentes.", "quote");
        }
        try {
            exchange.market(r.base(), r.quote());
        } catch (RuntimeException e) {
            throw new BusinessRuleException("Par de moedas não suportado.", "quote");
        }
    }

    /** Cotação de mercado real; nula se ainda não houve nenhuma atualização (a de reserva não dispara alerta). */
    private BigDecimal marketRate(String base, String quote) {
        try {
            ResolvedRate r = exchange.market(base, quote);
            return ResolvedRate.SOURCE_FALLBACK.equals(r.source()) ? null : r.rate();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private ExchangeRateAlert require(UUID userId, UUID id) {
        return alerts.findByIdAndUserId(id, userId).orElseThrow(() -> new NotFoundException("Alerta"));
    }

    static AlertResponse toResponse(ExchangeRateAlert a) {
        return new AlertResponse(a.getId(), a.getBase(), a.getQuote(), a.getCondition(), a.getThreshold(), a.getState(),
                a.getLastRate(), a.getLastTriggeredAt(), a.getTriggerCount(), a.isActive());
    }
}
