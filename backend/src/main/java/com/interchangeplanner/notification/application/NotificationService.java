package com.interchangeplanner.notification.application;

import com.interchangeplanner.notification.api.NotificationDtos.NotificationPage;
import com.interchangeplanner.notification.api.NotificationDtos.NotificationResponse;
import com.interchangeplanner.notification.application.NotificationDetector.Draft;
import com.interchangeplanner.notification.domain.Notification;
import com.interchangeplanner.notification.domain.NotificationCategory;
import com.interchangeplanner.notification.domain.NotificationRepository;
import com.interchangeplanner.shared.domain.UuidV7;
import com.interchangeplanner.shared.error.NotFoundException;
import com.interchangeplanner.user.application.UserClock;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gera e entrega notificações in-app. Os detectores das categorias ativas produzem rascunhos e o insert usa
 * {@code ON CONFLICT (user_id, dedupe_key) DO NOTHING}: rodar várias vezes no mesmo dia nunca duplica.
 * A entrega é in-app; outros canais (push, e-mail) entrariam aqui sem mudar os detectores.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final List<NotificationDetector> detectors;
    private final NotificationPreferenceService preferences;
    private final NotificationRepository notifications;
    private final JdbcClient jdbc;
    private final UserClock userClock;

    public NotificationService(List<NotificationDetector> detectors, NotificationPreferenceService preferences,
            NotificationRepository notifications, JdbcClient jdbc, UserClock userClock) {
        this.detectors = detectors;
        this.preferences = preferences;
        this.notifications = notifications;
        this.jdbc = jdbc;
        this.userClock = userClock;
    }

    /** Roda os detectores das categorias ativas para a pessoa; devolve quantas notificações novas foram criadas. */
    @Transactional
    public int generateFor(UUID userId) {
        LocalDate today = userClock.today(userId);
        Map<NotificationCategory, NotificationPreferenceService.Effective> prefs = preferences.effective(userId);
        int created = 0;
        for (NotificationDetector detector : detectors) {
            var pref = prefs.get(detector.category());
            if (pref == null || !pref.enabled()) {
                continue;
            }
            try {
                for (Draft draft : detector.detect(userId, today, pref.leadDays())) {
                    created += insert(userId, draft);
                }
            } catch (RuntimeException e) {
                // um detector com problema não impede os outros
                log.warn("Detector {} falhou para {}: {}", detector.category(), userId, e.getMessage());
            }
        }
        return created;
    }

    /** Insere se a chave ainda não existe para a pessoa; 1 se criou, 0 se já existia. */
    public int insert(UUID userId, Draft d) {
        Timestamp now = Timestamp.from(userClock.clock().instant());
        return jdbc.sql("""
                        INSERT INTO notification (id, user_id, category, severity, title, body, action_url, source_type,
                                                  source_id, dedupe_key, created_at, updated_at, version)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
                        ON CONFLICT (user_id, dedupe_key) DO NOTHING""")
                .params(UuidV7.generate(), userId, d.category().name(), d.severity().name(), cut(d.title(), 200),
                        cut(d.body(), 1000), cut(d.actionUrl(), 300), d.sourceType(), d.sourceId(),
                        cut(d.dedupeKey(), 200), now, now)
                .update();
    }

    @Transactional(readOnly = true)
    public NotificationPage list(UUID userId, boolean unreadOnly, int page, int size) {
        PageRequest pr = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        Page<Notification> result = unreadOnly
                ? notifications.findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(userId, pr)
                : notifications.findByUserIdOrderByCreatedAtDesc(userId, pr);
        return new NotificationPage(result.map(NotificationService::toResponse).getContent(), result.hasNext(),
                notifications.countByUserIdAndReadAtIsNull(userId));
    }

    @Transactional(readOnly = true)
    public long unreadCount(UUID userId) {
        return notifications.countByUserIdAndReadAtIsNull(userId);
    }

    @Transactional
    public NotificationResponse markRead(UUID userId, UUID id) {
        Notification n = notifications.findByIdAndUserId(id, userId).orElseThrow(() -> new NotFoundException("Notificação"));
        n.markRead(userClock.clock().instant());
        return toResponse(n);
    }

    @Transactional
    public int markAllRead(UUID userId) {
        return notifications.markAllRead(userId, userClock.clock().instant());
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        notifications.delete(notifications.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NotFoundException("Notificação")));
    }

    static NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(n.getId(), n.getCategory(), n.getSeverity(), n.getTitle(), n.getBody(),
                n.getActionUrl(), n.getReadAt() != null, n.getCreatedAt());
    }

    private static String cut(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
