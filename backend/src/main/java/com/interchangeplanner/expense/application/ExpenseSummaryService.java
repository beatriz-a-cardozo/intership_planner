package com.interchangeplanner.expense.application;

import com.interchangeplanner.exchange.api.ExchangeRateResponse;
import com.interchangeplanner.exchange.application.CurrencyConverter;
import com.interchangeplanner.exchange.application.ExchangeRateService;
import com.interchangeplanner.expense.api.ExpenseSummaryResponse;
import com.interchangeplanner.expense.api.ExpenseSummaryResponse.Group;
import com.interchangeplanner.expense.api.ExpenseSummaryResponse.GroupBy;
import com.interchangeplanner.expense.api.ExpenseSummaryResponse.Totals;
import com.interchangeplanner.expense.domain.CatalogItem;
import com.interchangeplanner.expense.domain.Expense;
import com.interchangeplanner.expense.domain.ExpenseStatus;
import com.interchangeplanner.expense.domain.Recurrence;
import com.interchangeplanner.objective.application.AdjustmentService;
import com.interchangeplanner.objective.application.ObjectiveService;
import com.interchangeplanner.objective.domain.ExpenseScope;
import com.interchangeplanner.objective.domain.Objective;
import com.interchangeplanner.shared.money.Money;
import com.interchangeplanner.shared.money.MoneyResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Agregações dos gastos de um objetivo, convertidas para a moeda do objetivo com UMA cotação por cálculo.
 * Gastos cancelados não entram em nenhum total.
 */
@Service
public class ExpenseSummaryService {

    /** Insumos para a meta/progresso do objetivo (moeda do objetivo, já no escopo configurado). */
    public record ObjectiveExpenseTotals(Money plannedInScope, Money paidInScope, Money adjustments) {
    }

    static final String NO_CYCLE = "none";

    private final ExpenseQueries queries;
    private final ObjectiveService objectives;
    private final AdjustmentService adjustments;
    private final ExpenseCategoryService categories;
    private final ExpenseCycleService cycles;
    private final ExchangeRateService exchange;

    public ExpenseSummaryService(ExpenseQueries queries, ObjectiveService objectives, AdjustmentService adjustments,
            ExpenseCategoryService categories, ExpenseCycleService cycles, ExchangeRateService exchange) {
        this.queries = queries;
        this.objectives = objectives;
        this.adjustments = adjustments;
        this.categories = categories;
        this.cycles = cycles;
        this.exchange = exchange;
    }

    @Transactional(readOnly = true)
    public ExpenseSummaryResponse summary(UUID userId, UUID objectiveId, GroupBy groupBy) {
        Objective objective = objectiveId == null
                ? objectives.resolve(userId, null).orElse(null)
                : objectives.requireOwned(userId, objectiveId);
        if (objective == null) {
            return null;
        }
        return summary(userId, objective, groupBy, exchange.converterFor(userId));
    }

    @Transactional(readOnly = true)
    public ExpenseSummaryResponse summary(UUID userId, Objective objective, GroupBy groupBy, CurrencyConverter converter) {
        String currency = objective.getCurrency();
        List<ComputedExpense> all = queries.forObjective(userId, objective.getId());
        List<ComputedExpense> active = all.stream().filter(c -> !c.isCancelled()).toList();

        Money total = Money.zero(currency);
        Money required = Money.zero(currency);
        Money paid = Money.zero(currency);
        Money remaining = Money.zero(currency);
        Money monthly = Money.zero(currency);
        for (ComputedExpense c : active) {
            Money t = converter.convert(c.total(), currency);
            total = total.plus(t);
            if (c.expense().isRequired()) {
                required = required.plus(t);
            }
            paid = paid.plus(converter.convert(c.paid(), currency));
            remaining = remaining.plus(converter.convert(c.remaining(), currency));
            if (c.expense().getRecurrence() == Recurrence.MONTHLY) {
                monthly = monthly.plus(converter.convert(
                        c.expense().unitPriceMoney().times(c.expense().getQuantity()), currency));
            }
        }
        Money adjustmentsTotal = adjustmentsTotal(userId, objective, converter);

        Totals totals = new Totals(MoneyResponse.of(total), MoneyResponse.of(required),
                MoneyResponse.of(total.minus(required)), MoneyResponse.of(paid), MoneyResponse.of(remaining),
                MoneyResponse.of(adjustmentsTotal), MoneyResponse.of(total.plus(adjustmentsTotal)),
                MoneyResponse.of(required.plus(adjustmentsTotal)), MoneyResponse.of(monthly), active.size(),
                all.size() - active.size());

        GroupBy by = groupBy == null ? GroupBy.CYCLE : groupBy;
        return new ExpenseSummaryResponse(objective.getId(), currency, totals, by,
                groups(userId, by, by == GroupBy.STATUS ? all : active, currency, converter),
                ExchangeRateResponse.of(converter));
    }

    /** Totais no escopo do objetivo (só obrigatórios ou todos), para o cálculo de meta e progresso. */
    @Transactional(readOnly = true)
    public ObjectiveExpenseTotals totalsFor(UUID userId, Objective objective, CurrencyConverter converter) {
        String currency = objective.getCurrency();
        boolean onlyRequired = objective.getExpenseScope() == ExpenseScope.REQUIRED_ONLY;
        Money planned = Money.zero(currency);
        Money paid = Money.zero(currency);
        for (ComputedExpense c : queries.forObjective(userId, objective.getId())) {
            if (c.isCancelled() || (onlyRequired && !c.expense().isRequired())) {
                continue;
            }
            planned = planned.plus(converter.convert(c.total(), currency));
            paid = paid.plus(converter.convert(c.paid(), currency));
        }
        return new ObjectiveExpenseTotals(planned, paid, adjustmentsTotal(userId, objective, converter));
    }

    private Money adjustmentsTotal(UUID userId, Objective objective, CurrencyConverter converter) {
        return adjustments.entities(userId, objective.getId()).stream()
                .map(a -> converter.convert(a.money(), objective.getCurrency()))
                .reduce(Money.zero(objective.getCurrency()), Money::plus);
    }

    private List<Group> groups(UUID userId, GroupBy by, List<ComputedExpense> items, String currency,
            CurrencyConverter converter) {
        Map<String, GroupAccumulator> acc = new LinkedHashMap<>();
        // Pré-cria os grupos na ordem de exibição (catálogo por sort_order; enums na ordem declarada)
        switch (by) {
            case CYCLE -> {
                cycles.entities(userId).forEach(c -> acc.put(c.getId().toString(), GroupAccumulator.of(c, currency)));
                acc.put(NO_CYCLE, new GroupAccumulator(NO_CYCLE, "Sem ciclo", null, null, currency));
            }
            case CATEGORY -> categories.entities(userId)
                    .forEach(c -> acc.put(c.getId().toString(), GroupAccumulator.of(c, currency)));
            case RECURRENCE -> {
                for (Recurrence r : Recurrence.values()) {
                    acc.put(r.name(), new GroupAccumulator(r.name(), recurrenceLabel(r), null, null, currency));
                }
            }
            case STATUS -> {
                for (ExpenseStatus s : ExpenseStatus.values()) {
                    acc.put(s.name(), new GroupAccumulator(s.name(), statusLabel(s), null, null, currency));
                }
            }
        }
        for (ComputedExpense c : items) {
            GroupAccumulator group = acc.get(keyOf(by, c));
            if (group != null) {
                group.add(c, converter);
            }
        }
        List<Group> result = new ArrayList<>();
        acc.values().stream().filter(g -> g.count > 0).forEach(g -> result.add(g.toGroup()));
        return result;
    }

    private static String keyOf(GroupBy by, ComputedExpense c) {
        Expense e = c.expense();
        return switch (by) {
            case CYCLE -> e.getCycleId() == null ? NO_CYCLE : e.getCycleId().toString();
            case CATEGORY -> e.getCategoryId().toString();
            case RECURRENCE -> e.getRecurrence().name();
            case STATUS -> c.status().name();
        };
    }

    static String recurrenceLabel(Recurrence r) {
        return switch (r) {
            case ONE_TIME -> "Único";
            case MONTHLY -> "Mensal";
            case SEMIANNUAL -> "Semestral";
            case ANNUAL -> "Anual";
            case PER_UNIT -> "Por unidade";
            case CUSTOM -> "Personalizado";
        };
    }

    static String statusLabel(ExpenseStatus s) {
        return switch (s) {
            case PENDING -> "Pendente";
            case PARTIALLY_PAID -> "Parcial";
            case PAID -> "Pago";
            case CANCELLED -> "Cancelado";
        };
    }

    private static final class GroupAccumulator {
        final String key;
        final String label;
        final String color;
        final String icon;
        Money total;
        Money required;
        Money paid;
        Money remaining;
        int count;

        GroupAccumulator(String key, String label, String color, String icon, String currency) {
            this.key = key;
            this.label = label;
            this.color = color;
            this.icon = icon;
            this.total = Money.zero(currency);
            this.required = Money.zero(currency);
            this.paid = Money.zero(currency);
            this.remaining = Money.zero(currency);
        }

        static GroupAccumulator of(CatalogItem item, String currency) {
            return new GroupAccumulator(item.getId().toString(), item.getName(), item.getColor(), item.getIcon(),
                    currency);
        }

        void add(ComputedExpense c, CurrencyConverter converter) {
            String currency = total.currency();
            Money t = converter.convert(c.total(), currency);
            total = total.plus(t);
            if (c.expense().isRequired()) {
                required = required.plus(t);
            }
            paid = paid.plus(converter.convert(c.paid(), currency));
            remaining = remaining.plus(converter.convert(c.remaining(), currency));
            count++;
        }

        Group toGroup() {
            return new Group(key, label, color, icon, MoneyResponse.of(total), MoneyResponse.of(required),
                    MoneyResponse.of(paid), MoneyResponse.of(remaining), count);
        }
    }
}
