package com.interchangeplanner.notification.application;

import com.interchangeplanner.expense.application.ExpenseTimelineProjector;
import com.interchangeplanner.finance.ledger.domain.FinanceTransaction;
import com.interchangeplanner.finance.ledger.domain.FinanceTransactionRepository;
import com.interchangeplanner.finance.ledger.domain.InstallmentPlan;
import com.interchangeplanner.finance.ledger.domain.InstallmentPlanRepository;
import com.interchangeplanner.notification.domain.Notification.Severity;
import com.interchangeplanner.notification.domain.NotificationCategory;
import com.interchangeplanner.notification.domain.NotificationRepository;
import com.interchangeplanner.objective.api.ObjectiveProgressResponse;
import com.interchangeplanner.objective.application.ObjectiveProgressService;
import com.interchangeplanner.objective.application.ObjectiveService;
import com.interchangeplanner.objective.domain.Objective;
import com.interchangeplanner.shared.domain.Priority;
import com.interchangeplanner.shared.money.Money;
import com.interchangeplanner.task.domain.Task;
import com.interchangeplanner.task.domain.TaskRepository;
import com.interchangeplanner.timeline.api.TimelineDtos.ItemStatus;
import com.interchangeplanner.timeline.api.TimelineDtos.TimelineItem;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Detectores de pagamentos, tarefas e marcos do objetivo. */
final class PlanningDetectors {

    private PlanningDetectors() {
    }

    /** Vencimentos de gastos (cada ocorrência) e de parcelas ainda não pagos. */
    @Component
    static class Payments implements NotificationDetector {

        private final ExpenseTimelineProjector expenses;
        private final FinanceTransactionRepository transactions;
        private final InstallmentPlanRepository plans;

        Payments(ExpenseTimelineProjector expenses, FinanceTransactionRepository transactions,
                InstallmentPlanRepository plans) {
            this.expenses = expenses;
            this.transactions = transactions;
            this.plans = plans;
        }

        @Override
        public NotificationCategory category() {
            return NotificationCategory.PAYMENTS;
        }

        @Override
        public List<Draft> detect(UUID userId, LocalDate today, List<Integer> leadDays) {
            LocalDate until = today.plusDays(LeadTime.max(leadDays));
            List<Draft> drafts = new ArrayList<>();
            for (TimelineItem item : expenses.project(userId, today, until, today)) {
                Integer lead = LeadTime.bucket(today, item.date(), leadDays);
                if (item.status() == ItemStatus.DONE || lead == null) {
                    continue;
                }
                String amount = item.amount() == null ? ""
                        : " · " + Texts.money(Money.of(item.amount().amount(), item.amount().currency()));
                drafts.add(new Draft(category(), NotificationDetector.severityFor(lead),
                        item.title() + " vence " + LeadTime.phrase(today, item.date()),
                        "Gasto · " + Texts.date(item.date()) + amount, "/expenses", "EXPENSE", item.sourceId(),
                        "payment:" + item.id() + ":" + lead + "d"));
            }

            List<FinanceTransaction> parcels = transactions
                    .findByUserIdAndOccurredOnBetweenOrderByOccurredOnAscCreatedAtAsc(userId, today, until).stream()
                    .filter(t -> t.getInstallmentPlanId() != null && !t.isConfirmed())
                    .toList();
            Map<UUID, InstallmentPlan> planIndex = plans.findAllById(parcels.stream()
                            .map(FinanceTransaction::getInstallmentPlanId).filter(Objects::nonNull).distinct().toList())
                    .stream().filter(p -> userId.equals(p.getUserId()))
                    .collect(Collectors.toMap(InstallmentPlan::getId, Function.identity()));
            for (FinanceTransaction t : parcels) {
                InstallmentPlan plan = planIndex.get(t.getInstallmentPlanId());
                Integer lead = LeadTime.bucket(today, t.getOccurredOn(), leadDays);
                if (plan == null || lead == null) {
                    continue;
                }
                drafts.add(new Draft(category(), NotificationDetector.severityFor(lead),
                        "Parcela " + t.getInstallmentNumber() + "/" + plan.getInstallmentsCount() + " de "
                                + plan.getDescription() + " vence " + LeadTime.phrase(today, t.getOccurredOn()),
                        Texts.date(t.getOccurredOn()) + " · " + Texts.money(t.money()), "/finance?tab=installments",
                        "INSTALLMENT", plan.getId(), "payment:installment:" + t.getId() + ":" + lead + "d"));
            }
            return drafts;
        }
    }

    /** Prazos das tarefas abertas. Tarefa urgente é sempre crítica. */
    @Component
    static class Tasks implements NotificationDetector {

        private final TaskRepository tasks;

        Tasks(TaskRepository tasks) {
            this.tasks = tasks;
        }

        @Override
        public NotificationCategory category() {
            return NotificationCategory.TASKS;
        }

        @Override
        public List<Draft> detect(UUID userId, LocalDate today, List<Integer> leadDays) {
            List<Draft> drafts = new ArrayList<>();
            for (Task t : tasks.findByUserIdAndDueDateBetween(userId, today, today.plusDays(LeadTime.max(leadDays)))) {
                Integer lead = LeadTime.bucket(today, t.getDueDate(), leadDays);
                if (t.isDone() || lead == null) {
                    continue;
                }
                Severity severity = t.getPriority() == Priority.URGENT ? Severity.CRITICAL
                        : NotificationDetector.severityFor(lead);
                drafts.add(new Draft(category(), severity,
                        "Tarefa para " + LeadTime.phrase(today, t.getDueDate()) + ": " + t.getTitle(),
                        "Prazo " + Texts.date(t.getDueDate()), "/tasks", "TASK", t.getId(),
                        "task:" + t.getId() + ":" + t.getDueDate() + ":" + lead + "d"));
            }
            return drafts;
        }
    }

    /**
     * Marcos do objetivo principal: 25%, 50%, 75%, 90% e 100%. Só o maior marco atingido gera aviso (quem já começa em
     * 60% recebe "50%", não três avisos), cada marco uma vez, e no máximo uma mensagem motivacional por semana.
     */
    @Component
    static class Milestones implements NotificationDetector {

        static final int[] MARKS = {100, 90, 75, 50, 25};

        private final ObjectiveService objectives;
        private final ObjectiveProgressService progress;
        private final NotificationRepository notifications;
        private final Clock clock;

        Milestones(ObjectiveService objectives, ObjectiveProgressService progress, NotificationRepository notifications,
                Clock clock) {
            this.objectives = objectives;
            this.progress = progress;
            this.notifications = notifications;
            this.clock = clock;
        }

        @Override
        public NotificationCategory category() {
            return NotificationCategory.MOTIVATIONAL;
        }

        @Override
        public List<Draft> detect(UUID userId, LocalDate today, List<Integer> leadDays) {
            Objective objective = objectives.resolve(userId, null).orElse(null);
            if (objective == null) {
                return List.of();
            }
            ObjectiveProgressResponse p = progress.progress(userId, objective.getId());
            if (p.target().amount().signum() == 0) {
                return List.of();
            }
            Integer mark = reached(p.percentComplete().doubleValue());
            if (mark == null || notifications.existsByUserIdAndCategoryAndCreatedAtAfter(userId, category(),
                    clock.instant().minus(Duration.ofDays(7)))) {
                return List.of();
            }
            String remaining = Texts.money(Money.of(p.remaining().amount(), p.remaining().currency()));
            String title = mark == 100 ? "Meta alcançada: " + objective.getName() + "!"
                    : "Você chegou a " + mark + "% da meta";
            String body = mark == 100 ? "Tudo o que você planejou já está guardado ou pago."
                    : "Faltam " + remaining + " para " + objective.getName() + ". Continue assim!";
            return List.of(new Draft(category(), Severity.INFO, title, body, "/objectives/" + objective.getId(),
                    "OBJECTIVE", objective.getId(), "milestone:" + objective.getId() + ":" + mark));
        }

        static Integer reached(double percent) {
            for (int m : MARKS) {
                if (percent >= m) {
                    return m;
                }
            }
            return null;
        }
    }
}
