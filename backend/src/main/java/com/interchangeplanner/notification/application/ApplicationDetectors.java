package com.interchangeplanner.notification.application;

import com.interchangeplanner.applications.api.ApplicationDtos.ApplicationResponse;
import com.interchangeplanner.applications.application.ApplicationService;
import com.interchangeplanner.applications.domain.ApplicationRequest;
import com.interchangeplanner.applications.domain.ApplicationRequestRepository;
import com.interchangeplanner.applications.domain.ApplicationStatus;
import com.interchangeplanner.notification.domain.Notification.Severity;
import com.interchangeplanner.notification.domain.NotificationCategory;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Detectores das candidaturas: prazos (inscrição e solicitações) e respostas esperadas. */
final class ApplicationDetectors {

    private ApplicationDetectors() {
    }

    static String programUrl(ApplicationResponse a) {
        return "/programs/" + a.program().id() + "?tab=application";
    }

    /** Prazo de inscrição das candidaturas ainda não enviadas e prazos das solicitações pendentes. */
    @Component
    static class Deadlines implements NotificationDetector {

        private final ApplicationService applications;
        private final ApplicationRequestRepository requests;

        Deadlines(ApplicationService applications, ApplicationRequestRepository requests) {
            this.applications = applications;
            this.requests = requests;
        }

        @Override
        public NotificationCategory category() {
            return NotificationCategory.DEADLINES;
        }

        @Override
        public List<Draft> detect(UUID userId, LocalDate today, List<Integer> leadDays) {
            List<ApplicationResponse> active = applications.list(userId, null, null, false);
            List<Draft> drafts = new ArrayList<>();
            for (ApplicationResponse a : active) {
                boolean submitted = a.submittedAt() != null || a.status() != ApplicationStatus.AWAITING_OPENING;
                LocalDate deadline = a.deadline() != null ? a.deadline() : a.window() == null ? null : a.window().closesOn();
                Integer lead = LeadTime.bucket(today, deadline, leadDays);
                if (submitted || lead == null) {
                    continue;
                }
                drafts.add(new Draft(category(), NotificationDetector.severityFor(lead),
                        "Inscrição fecha " + LeadTime.phrase(today, deadline) + ": " + a.program().name(),
                        a.program().universityName() + " · prazo " + Texts.date(deadline), programUrl(a), "APPLICATION",
                        a.id(), "deadline:application:" + a.id() + ":" + deadline + ":" + lead + "d"));
            }
            Map<UUID, ApplicationResponse> byId = active.stream()
                    .collect(Collectors.toMap(ApplicationResponse::id, Function.identity()));
            for (ApplicationRequest r : requests.findByUserIdAndDueDateBetween(userId, today,
                    today.plusDays(LeadTime.max(leadDays)))) {
                ApplicationResponse a = byId.get(r.getApplicationId());
                Integer lead = LeadTime.bucket(today, r.getDueDate(), leadDays);
                if (a == null || !r.isPending() || lead == null) {
                    continue;
                }
                drafts.add(new Draft(category(), NotificationDetector.severityFor(lead),
                        "Entregar " + LeadTime.phrase(today, r.getDueDate()) + ": " + r.getDescription(),
                        "Pedido de " + a.program().universityName() + " · até " + Texts.date(r.getDueDate()),
                        programUrl(a), "REQUEST", r.getId(),
                        "deadline:request:" + r.getId() + ":" + r.getDueDate() + ":" + lead + "d"));
            }
            return drafts;
        }
    }

    /** Data provável da resposta das candidaturas que ainda não tiveram resposta. */
    @Component
    static class Responses implements NotificationDetector {

        private final ApplicationService applications;

        Responses(ApplicationService applications) {
            this.applications = applications;
        }

        @Override
        public NotificationCategory category() {
            return NotificationCategory.RESPONSES;
        }

        @Override
        public List<Draft> detect(UUID userId, LocalDate today, List<Integer> leadDays) {
            List<Draft> drafts = new ArrayList<>();
            for (ApplicationResponse a : applications.list(userId, null, null, false)) {
                LocalDate expected = a.responseLikelyDate() != null ? a.responseLikelyDate() : a.responseMinDate();
                Integer lead = LeadTime.bucket(today, expected, leadDays);
                if (a.actualResponseDate() != null || lead == null) {
                    continue;
                }
                drafts.add(new Draft(category(), Severity.INFO,
                        "Resposta esperada " + LeadTime.phrase(today, expected) + ": " + a.program().name(),
                        a.program().universityName() + " · data provável " + Texts.date(expected), programUrl(a),
                        "APPLICATION", a.id(), "response:" + a.id() + ":" + expected + ":" + lead + "d"));
            }
            return drafts;
        }
    }
}
