package com.interchangeplanner.expense.application;

import com.interchangeplanner.expense.domain.Expense;
import com.interchangeplanner.expense.domain.ExpensePaymentRepository;
import com.interchangeplanner.expense.domain.ExpenseRepository;
import com.interchangeplanner.expense.domain.PaidSum;
import com.interchangeplanner.expense.domain.Recurrence;
import com.interchangeplanner.shared.money.Money;
import com.interchangeplanner.shared.money.MoneyResponse;
import com.interchangeplanner.timeline.api.TimelineDtos.Kind;
import com.interchangeplanner.timeline.api.TimelineDtos.Source;
import com.interchangeplanner.timeline.api.TimelineDtos.TimelineItem;
import com.interchangeplanner.timeline.application.TimelineProjector;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Vencimentos dos gastos com data. Gastos mensais/semestrais/anuais viram uma ocorrência por período a
 * partir do 1º vencimento (até as ocorrências ou a data final); cada ocorrência está paga quando o total pago cobre
 * ela e as anteriores. Cancelados não aparecem.
 */
@Component
public class ExpenseTimelineProjector implements TimelineProjector {

    private final ExpenseRepository expenses;
    private final ExpensePaymentRepository payments;

    public ExpenseTimelineProjector(ExpenseRepository expenses, ExpensePaymentRepository payments) {
        this.expenses = expenses;
        this.payments = payments;
    }

    @Override
    public Set<Source> sources() {
        return Set.of(Source.EXPENSE);
    }

    @Override
    public List<TimelineItem> project(UUID userId, LocalDate from, LocalDate to, LocalDate today) {
        List<TimelineItem> items = new ArrayList<>();
        List<Expense> withDue = expenses.findByUserIdAndDueDateIsNotNull(userId).stream()
                .filter(e -> !e.isCancelled())
                .toList();
        if (!withDue.isEmpty()) {
            Map<UUID, BigDecimal> paid = payments.paidSums(userId, withDue.stream().map(Expense::getId).toList()).stream()
                    .collect(Collectors.toMap(PaidSum::expenseId, PaidSum::paid));
            for (Expense e : withDue) {
                items.addAll(dueItems(e, Money.of(paid.getOrDefault(e.getId(), BigDecimal.ZERO), e.getCurrency()),
                        from, to, today));
            }
        }

        return items;
    }

    private static List<TimelineItem> dueItems(Expense e, Money paid, LocalDate from, LocalDate to, LocalDate today) {
        List<LocalDate> dates = occurrenceDates(e.getRecurrence(), e.getDueDate(), e.getEndDate(), e.getOccurrences());
        boolean single = dates.size() == 1;
        Money total = ExpenseCalculator.total(e.unitPriceMoney(), e.getQuantity(), e.getOccurrences());
        Money each = single ? total : e.unitPriceMoney().times(e.getQuantity());
        List<TimelineItem> items = new ArrayList<>();
        for (int k = 0; k < dates.size(); k++) {
            LocalDate date = dates.get(k);
            if (!TimelineProjector.within(date, from, to)) {
                continue;
            }
            boolean covered = paid.compareTo(each.times(k + 1L)) >= 0;
            String id = "expense:" + e.getId() + ":due" + (single ? "" : ":" + k);
            String subtitle = single ? "Vencimento" : "Vencimento · " + (k + 1) + "/" + dates.size();
            items.add(new TimelineItem(id, Source.EXPENSE, Kind.PAYMENT_DUE, date, null, e.getName(), subtitle,
                    TimelineProjector.status(covered, date, today), null, MoneyResponse.of(each), e.getId(), null, null,
                    null));
        }
        return items;
    }

    /**
     * Datas de vencimento: mensal a cada 1 mês, semestral a cada 6, anual a cada 12, sempre a partir do 1º
     * vencimento (dia 31 não escorrega) e sem passar da data final; as demais periodicidades têm uma data só.
     */
    public static List<LocalDate> occurrenceDates(Recurrence recurrence, LocalDate first, LocalDate end, int occurrences) {
        int step = switch (recurrence) {
            case MONTHLY -> 1;
            case SEMIANNUAL -> 6;
            case ANNUAL -> 12;
            default -> 0;
        };
        if (step == 0 || occurrences <= 1) {
            return List.of(first);
        }
        List<LocalDate> dates = new ArrayList<>(occurrences);
        for (int k = 0; k < occurrences; k++) {
            LocalDate d = first.plusMonths((long) step * k);
            if (end != null && d.isAfter(end)) {
                break;
            }
            dates.add(d);
        }
        return dates;
    }
}
