package com.interchangeplanner.applications.application;

import com.interchangeplanner.applications.api.ApplicationDtos.ApplicationResponse;
import com.interchangeplanner.applications.domain.ApplicationRequest;
import com.interchangeplanner.applications.domain.ApplicationRequestRepository;
import com.interchangeplanner.applications.domain.ApplicationStatus;
import com.interchangeplanner.timeline.api.TimelineDtos.ItemStatus;
import com.interchangeplanner.timeline.api.TimelineDtos.Kind;
import com.interchangeplanner.timeline.api.TimelineDtos.Source;
import com.interchangeplanner.timeline.api.TimelineDtos.TimelineItem;
import com.interchangeplanner.timeline.application.TimelineProjector;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Candidaturas ativas na timeline: abertura das inscrições, prazo (o da candidatura ou o da janela do programa),
 * resposta esperada (data provável, com a faixa mín–máx) ou recebida, e prazos das solicitações da universidade.
 * Candidaturas arquivadas não aparecem.
 */
@Component
public class ApplicationTimelineProjector implements TimelineProjector {

    private final ApplicationService applications;
    private final ApplicationRequestRepository requests;

    public ApplicationTimelineProjector(ApplicationService applications, ApplicationRequestRepository requests) {
        this.applications = applications;
        this.requests = requests;
    }

    @Override
    public Set<Source> sources() {
        return Set.of(Source.APPLICATION, Source.REQUEST);
    }

    @Override
    public List<TimelineItem> project(UUID userId, LocalDate from, LocalDate to, LocalDate today) {
        List<ApplicationResponse> active = applications.list(userId, null, null, false);
        List<TimelineItem> items = new ArrayList<>();
        for (ApplicationResponse a : active) {
            items.addAll(applicationItems(a, from, to, today));
        }
        Map<UUID, ApplicationResponse> byId = active.stream()
                .collect(Collectors.toMap(ApplicationResponse::id, Function.identity()));
        for (ApplicationRequest r : requests.findByUserIdAndDueDateBetween(userId, from, to)) {
            ApplicationResponse a = byId.get(r.getApplicationId());
            if (a == null) {
                continue;
            }
            items.add(new TimelineItem("request:" + r.getId() + ":due", Source.REQUEST, Kind.DEADLINE, r.getDueDate(),
                    null, r.getDescription(), "Solicitação · " + a.program().name(),
                    TimelineProjector.status(!r.isPending(), r.getDueDate(), today), null, null, a.id(),
                    a.program().id(), null, null));
        }
        return items;
    }

    public static List<TimelineItem> applicationItems(ApplicationResponse a, LocalDate from, LocalDate to, LocalDate today) {
        List<TimelineItem> items = new ArrayList<>();
        String program = a.program().name();
        String university = a.program().universityName();
        boolean submitted = a.submittedAt() != null || a.status() != ApplicationStatus.AWAITING_OPENING;

        LocalDate opens = a.window() == null ? null : a.window().opensOn();
        if (!submitted && TimelineProjector.within(opens, from, to)) {
            items.add(item(a, "opens", Kind.WINDOW_OPENS, opens, null, "Inscrições abrem: " + program, university,
                    opens.isAfter(today) ? ItemStatus.PENDING : ItemStatus.DONE));
        }

        LocalDate deadline = a.deadline() != null ? a.deadline() : a.window() == null ? null : a.window().closesOn();
        if (TimelineProjector.within(deadline, from, to)) {
            items.add(item(a, "deadline", Kind.DEADLINE, deadline, null, "Prazo de inscrição: " + program, university,
                    TimelineProjector.status(submitted, deadline, today)));
        }

        if (a.actualResponseDate() != null) {
            if (TimelineProjector.within(a.actualResponseDate(), from, to)) {
                items.add(item(a, "response", Kind.RESPONSE, a.actualResponseDate(), null, "Resposta: " + program,
                        university, ItemStatus.DONE));
            }
        } else {
            LocalDate expected = firstNonNull(a.responseLikelyDate(), a.responseMinDate(), a.responseMaxDate());
            if (TimelineProjector.within(expected, from, to)) {
                LocalDate latest = a.responseMaxDate() != null && a.responseMaxDate().isAfter(expected)
                        ? a.responseMaxDate() : null;
                LocalDate lastChance = latest != null ? latest : expected;
                items.add(item(a, "response", Kind.RESPONSE, expected, latest, "Resposta esperada: " + program,
                        university, lastChance.isBefore(today) ? ItemStatus.OVERDUE : ItemStatus.PENDING));
            }
        }
        return items;
    }

    private static TimelineItem item(ApplicationResponse a, String suffix, Kind kind, LocalDate date, LocalDate end,
            String title, String subtitle, ItemStatus status) {
        return new TimelineItem("application:" + a.id() + ":" + suffix, Source.APPLICATION, kind, date, end, title,
                subtitle, status, null, null, a.id(), a.program().id(), null, null);
    }

    @SafeVarargs
    private static <T> T firstNonNull(T... values) {
        for (T v : values) {
            if (v != null) {
                return v;
            }
        }
        return null;
    }
}
