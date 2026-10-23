package com.interchangeplanner.timeline.application;

import com.interchangeplanner.shared.error.BusinessRuleException;
import com.interchangeplanner.timeline.api.TimelineDtos.ItemStatus;
import com.interchangeplanner.timeline.api.TimelineDtos.Source;
import com.interchangeplanner.timeline.api.TimelineDtos.TimelineItem;
import com.interchangeplanner.timeline.api.TimelineDtos.TimelineResponse;
import com.interchangeplanner.user.application.UserClock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Junta os itens de todos os {@link TimelineProjector} num intervalo, em ordem: data, depois tipo (prazos antes
 * de pagamentos, respostas e tarefas), depois título. Ids determinísticos garantem que um item aparece uma vez.
 */
@Service
public class TimelineService {

    /** Intervalo máximo de uma consulta (evita varrer anos de recorrências). */
    static final long MAX_DAYS = 3 * 366;

    private static final Comparator<TimelineItem> ORDER = Comparator.comparing(TimelineItem::date)
            .thenComparing(TimelineItem::kind)
            .thenComparing(TimelineItem::title, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(TimelineItem::id);

    private final List<TimelineProjector> projectors;
    private final UserClock userClock;

    public TimelineService(List<TimelineProjector> projectors, UserClock userClock) {
        this.projectors = projectors;
        this.userClock = userClock;
    }

    /** Padrão: do mês anterior até 12 meses à frente. */
    @Transactional(readOnly = true)
    public TimelineResponse timeline(UUID userId, LocalDate from, LocalDate to, Set<Source> sources) {
        LocalDate today = userClock.today(userId);
        LocalDate start = from != null ? from : today.minusMonths(1).withDayOfMonth(1);
        LocalDate end = to != null ? to : today.plusMonths(12);
        if (end.isBefore(start)) {
            throw new BusinessRuleException("O fim do período deve ser depois do início.", "to");
        }
        if (ChronoUnit.DAYS.between(start, end) > MAX_DAYS) {
            throw new BusinessRuleException("Consulte no máximo 3 anos por vez.", "to");
        }
        Set<Source> wanted = sources == null || sources.isEmpty() ? EnumSet.allOf(Source.class) : sources;
        return new TimelineResponse(start, end, today, collect(userId, start, end, today, wanted));
    }

    /**
     * Próximos itens para o dashboard: o que está atrasado (últimos 30 dias) e o que vence nos próximos 60, sem os
     * concluídos. Atrasados primeiro.
     */
    @Transactional(readOnly = true)
    public List<TimelineItem> upcoming(UUID userId, int limit) {
        LocalDate today = userClock.today(userId);
        return collect(userId, today.minusDays(30), today.plusDays(60), today, EnumSet.allOf(Source.class)).stream()
                .filter(i -> i.status() != ItemStatus.DONE)
                .sorted(Comparator.comparing((TimelineItem i) -> i.status() != ItemStatus.OVERDUE).thenComparing(ORDER))
                .limit(limit)
                .toList();
    }

    private List<TimelineItem> collect(UUID userId, LocalDate from, LocalDate to, LocalDate today, Set<Source> wanted) {
        List<List<TimelineItem>> parts = projectors.stream()
                .filter(p -> p.sources().stream().anyMatch(wanted::contains))
                .map(p -> p.project(userId, from, to, today))
                .toList();
        return merge(parts).stream().filter(i -> wanted.contains(i.source())).toList();
    }

    /** Une as listas dos projetores (o primeiro item de cada id vence) e ordena. */
    public static List<TimelineItem> merge(Collection<List<TimelineItem>> parts) {
        Map<String, TimelineItem> byId = new LinkedHashMap<>();
        parts.forEach(list -> list.forEach(i -> byId.putIfAbsent(i.id(), i)));
        return byId.values().stream().sorted(ORDER).toList();
    }
}
