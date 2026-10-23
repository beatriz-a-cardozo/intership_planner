package com.interchangeplanner.dashboard;

import com.interchangeplanner.applications.api.ApplicationDtos.ApplicationResponse;
import com.interchangeplanner.applications.domain.ApplicationStatus;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Resumo das candidaturas ativas para o dashboard: contagem por resultado e os próximos marcos
 * (prazo de inscrição, resposta provável, prazo de solicitação), a partir de hoje.
 */
public record ApplicationsOverview(int active, int pending, int accepted, int rejected, int waitlisted,
        List<Upcoming> upcoming) {

    public enum Kind {
        DEADLINE,
        RESPONSE,
        REQUEST
    }

    public record Upcoming(UUID applicationId, String programName, String universityName, LocalDate date, Kind kind) {
    }

    static final int MAX_UPCOMING = 5;

    public static ApplicationsOverview of(List<ApplicationResponse> list, LocalDate today) {
        int pending = 0;
        int accepted = 0;
        int rejected = 0;
        int waitlisted = 0;
        List<Upcoming> upcoming = new ArrayList<>();
        for (ApplicationResponse a : list) {
            switch (a.result()) {
                case ACCEPTED -> accepted++;
                case REJECTED -> rejected++;
                case WAITLISTED -> waitlisted++;
                default -> pending++;
            }
            if (a.result() != ApplicationStatus.Result.PENDING) {
                continue;
            }
            add(upcoming, a, a.deadline(), Kind.DEADLINE, today);
            add(upcoming, a, a.responseLikelyDate(), Kind.RESPONSE, today);
            add(upcoming, a, a.nextRequestDue(), Kind.REQUEST, today);
        }
        upcoming.sort(Comparator.comparing(Upcoming::date));
        return new ApplicationsOverview(list.size(), pending, accepted, rejected, waitlisted,
                upcoming.stream().limit(MAX_UPCOMING).toList());
    }

    private static void add(List<Upcoming> list, ApplicationResponse a, LocalDate date, Kind kind, LocalDate today) {
        if (date != null && !date.isBefore(today)) {
            list.add(new Upcoming(a.id(), a.program().name(), a.program().universityName(), date, kind));
        }
    }
}
