package com.interchangeplanner.timeline.application;

import com.interchangeplanner.timeline.api.TimelineDtos.ItemStatus;
import com.interchangeplanner.timeline.api.TimelineDtos.Source;
import com.interchangeplanner.timeline.api.TimelineDtos.TimelineItem;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Projeta itens da timeline a partir das entidades de um módulo, calculados na leitura (nada é copiado para
 * outra tabela, então nunca ficam dessincronizados). Cada implementação faz no máximo uma consulta por origem,
 * filtrada pelo intervalo pedido.
 */
public interface TimelineProjector {

    /** Origens produzidas: o serviço não chama o projetor se nenhuma delas foi pedida. */
    Set<Source> sources();

    /** Itens com data em [from, to] (inclusive). {@code today} é o dia no fuso do usuário. */
    List<TimelineItem> project(UUID userId, LocalDate from, LocalDate to, LocalDate today);

    /** Status de algo que deveria ser feito até {@code date}. */
    static ItemStatus status(boolean done, LocalDate date, LocalDate today) {
        if (done) {
            return ItemStatus.DONE;
        }
        return date.isBefore(today) ? ItemStatus.OVERDUE : ItemStatus.PENDING;
    }

    static boolean within(LocalDate date, LocalDate from, LocalDate to) {
        return date != null && !date.isBefore(from) && !date.isAfter(to);
    }
}
