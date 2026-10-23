package com.interchangeplanner.notification.domain;

import java.util.List;

/**
 * Categoria da notificação, com as antecedências padrão (dias antes da data). Categorias sem data própria (câmbio,
 * motivacional) não usam antecedência.
 */
public enum NotificationCategory {
    /** Prazos de inscrição e de solicitações da universidade. */
    DEADLINES(List.of(30, 7, 1)),
    /** Vencimentos de gastos e de parcelas. */
    PAYMENTS(List.of(7, 1)),
    /** Data provável de resposta das candidaturas. */
    RESPONSES(List.of(7, 0)),
    /** Prazos das tarefas. */
    TASKS(List.of(1, 0)),
    /** Alertas de cotação definidos pela pessoa. */
    EXCHANGE(List.of()),
    /** Marcos do objetivo (25%, 50%, 75%, 90%, 100%), no máximo um por semana. */
    MOTIVATIONAL(List.of());

    private final List<Integer> defaultLeadDays;

    NotificationCategory(List<Integer> defaultLeadDays) {
        this.defaultLeadDays = defaultLeadDays;
    }

    public List<Integer> defaultLeadDays() {
        return defaultLeadDays;
    }

    public boolean usesLeadDays() {
        return !defaultLeadDays.isEmpty();
    }
}
