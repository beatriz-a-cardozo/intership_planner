package com.interchangeplanner.notification.application;

import com.interchangeplanner.notification.domain.Notification.Severity;
import com.interchangeplanner.notification.domain.NotificationCategory;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Detecta o que merece aviso para uma pessoa num dia. Cada detector cuida de uma categoria e devolve rascunhos com
 * chave de deduplicação estável: rodar de novo no mesmo dia (ou depois de reiniciar) não repete avisos.
 */
public interface NotificationDetector {

    /**
     * @param dedupeKey única por pessoa (ex.: {@code deadline:application:{id}:2027-05-31:7d})
     * @param actionUrl rota do frontend que resolve o aviso
     */
    record Draft(NotificationCategory category, Severity severity, String title, String body, String actionUrl,
            String sourceType, UUID sourceId, String dedupeKey) {
    }

    NotificationCategory category();

    List<Draft> detect(UUID userId, LocalDate today, List<Integer> leadDays);

    /** Prazo a ≤ 1 dia é crítico; a ≤ 7, atenção. */
    static Severity severityFor(int lead) {
        return lead <= 1 ? Severity.CRITICAL : lead <= 7 ? Severity.WARNING : Severity.INFO;
    }
}
