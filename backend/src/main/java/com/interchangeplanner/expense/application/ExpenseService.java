package com.interchangeplanner.expense.application;

import com.interchangeplanner.currency.CurrencyService;
import com.interchangeplanner.exchange.application.CurrencyConverter;
import com.interchangeplanner.exchange.application.ExchangeRateService;
import com.interchangeplanner.expense.api.ExpenseDetailResponse;
import com.interchangeplanner.expense.api.ExpenseRequest;
import com.interchangeplanner.expense.api.ExpenseResponse;
import com.interchangeplanner.expense.api.PaymentRequest;
import com.interchangeplanner.expense.api.PaymentResponse;
import com.interchangeplanner.expense.api.PreviewTotalRequest;
import com.interchangeplanner.expense.api.PreviewTotalResponse;
import com.interchangeplanner.expense.domain.CatalogItem;
import com.interchangeplanner.expense.domain.Expense;
import com.interchangeplanner.expense.domain.ExpenseCategory;
import com.interchangeplanner.expense.domain.ExpenseCycle;
import com.interchangeplanner.expense.domain.ExpensePayment;
import com.interchangeplanner.expense.domain.ExpensePaymentRepository;
import com.interchangeplanner.expense.domain.ExpenseRepository;
import com.interchangeplanner.expense.domain.ExpenseStatus;
import com.interchangeplanner.objective.application.ObjectiveService;
import com.interchangeplanner.objective.domain.Objective;
import com.interchangeplanner.shared.error.BusinessRuleException;
import com.interchangeplanner.shared.error.NotFoundException;
import com.interchangeplanner.shared.money.Money;
import com.interchangeplanner.shared.money.MoneyResponse;
import com.interchangeplanner.user.application.UserClock;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExpenseService {

    /** Filtros da listagem; todos opcionais. Sem objetivo = objetivo principal. */
    public record Filter(UUID objectiveId, UUID categoryId, UUID cycleId, ExpenseStatus status, Boolean required,
            String currency, LocalDate from, LocalDate to, String q) {
    }

    private static final Comparator<ComputedExpense> ORDER = Comparator
            .comparing((ComputedExpense c) -> c.expense().getDueDate(), Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(c -> c.expense().getName(), String.CASE_INSENSITIVE_ORDER);

    private final ExpenseRepository expenses;
    private final ExpensePaymentRepository payments;
    private final ExpenseQueries queries;
    private final ObjectiveService objectives;
    private final ExpenseCategoryService categories;
    private final ExpenseCycleService cycles;
    private final CurrencyService currencies;
    private final ExchangeRateService exchange;
    private final Clock clock;
    private final UserClock userClock;

    public ExpenseService(ExpenseRepository expenses, ExpensePaymentRepository payments, ExpenseQueries queries,
            ObjectiveService objectives, ExpenseCategoryService categories, ExpenseCycleService cycles,
            CurrencyService currencies, ExchangeRateService exchange, UserClock userClock) {
        this.expenses = expenses;
        this.payments = payments;
        this.queries = queries;
        this.objectives = objectives;
        this.categories = categories;
        this.cycles = cycles;
        this.currencies = currencies;
        this.exchange = exchange;
        this.clock = userClock.clock();
        this.userClock = userClock;
    }

    // ------------------------------------------------------------------ leitura

    @Transactional(readOnly = true)
    public List<ExpenseResponse> list(UUID userId, Filter filter) {
        Objective objective = objectives.resolve(userId, filter.objectiveId()).orElse(null);
        if (objective == null) {
            return List.of();
        }
        Mapper mapper = mapper(userId, objective);
        String q = filter.q() == null || filter.q().isBlank() ? null : filter.q().strip().toLowerCase(Locale.ROOT);
        return queries.forObjective(userId, objective.getId()).stream()
                .filter(c -> matches(c, filter, q))
                .sorted(ORDER)
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ExpenseDetailResponse get(UUID userId, UUID id) {
        Expense expense = require(userId, id);
        Objective objective = objectives.requireOwned(userId, expense.getObjectiveId());
        return detail(userId, expense, objective);
    }

    @Transactional(readOnly = true)
    public PreviewTotalResponse preview(UUID userId, PreviewTotalRequest request) {
        Objective objective = objectives.requireOwned(userId, request.objectiveId());
        requireCurrency(request.currency());
        int quantity = request.quantity() == null ? 1 : request.quantity();
        int occurrences = ExpenseCalculator.resolveOccurrences(request.recurrence(), request.occurrences(),
                objective.getHorizonMonths());
        Money total = ExpenseCalculator.total(Money.of(request.unitPrice(), request.currency()), quantity, occurrences);
        CurrencyConverter converter = exchange.converterFor(userId);
        return new PreviewTotalResponse(occurrences,
                ExpenseCalculator.suggestedOccurrences(request.recurrence(), objective.getHorizonMonths()),
                objective.getHorizonMonths(), MoneyResponse.of(total),
                MoneyResponse.of(converter.convert(total, objective.getCurrency())));
    }

    // ------------------------------------------------------------------ escrita

    @Transactional
    public ExpenseDetailResponse create(UUID userId, ExpenseRequest request) {
        Objective objective = objectives.requireOwned(userId, request.objectiveId());
        Expense expense = expenses.save(Expense.create(userId, toData(userId, objective, request)));
        return detail(userId, expense, objective);
    }

    @Transactional
    public ExpenseDetailResponse update(UUID userId, UUID id, ExpenseRequest request) {
        Expense expense = require(userId, id);
        Objective objective = objectives.requireOwned(userId, request.objectiveId());
        expense.update(toData(userId, objective, request));
        return detail(userId, expense, objective);
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        expenses.delete(require(userId, id));
    }

    @Transactional
    public ExpenseDetailResponse cancel(UUID userId, UUID id) {
        Expense expense = require(userId, id);
        expense.cancel(clock.instant());
        return detail(userId, expense, objectives.requireOwned(userId, expense.getObjectiveId()));
    }

    @Transactional
    public ExpenseDetailResponse restore(UUID userId, UUID id) {
        Expense expense = require(userId, id);
        expense.restore();
        return detail(userId, expense, objectives.requireOwned(userId, expense.getObjectiveId()));
    }

    // ------------------------------------------------------------------ pagamentos

    @Transactional(readOnly = true)
    public List<PaymentResponse> payments(UUID userId, UUID expenseId) {
        Expense expense = require(userId, expenseId);
        return payments.findByUserIdAndExpenseIdOrderByPaidOnAscCreatedAtAsc(userId, expenseId).stream()
                .map(p -> PaymentResponse.from(p, expense.getCurrency()))
                .toList();
    }

    @Transactional
    public ExpenseDetailResponse addPayment(UUID userId, UUID expenseId, PaymentRequest request) {
        Expense expense = require(userId, expenseId);
        if (expense.isCancelled()) {
            throw new BusinessRuleException("Não é possível pagar um gasto cancelado. Reative-o primeiro.", "expenseId");
        }
        if (request.paidOn().isAfter(userClock.today(userId))) {
            throw new BusinessRuleException("A data do pagamento não pode estar no futuro.", "paidOn");
        }
        requireCurrency(request.currency());
        BigDecimal rate;
        if (request.currency().equals(expense.getCurrency())) {
            if (request.rateToExpenseCurrency() != null && request.rateToExpenseCurrency().compareTo(BigDecimal.ONE) != 0) {
                throw new BusinessRuleException("Pagamento na mesma moeda do gasto não usa taxa.", "rateToExpenseCurrency");
            }
            rate = BigDecimal.ONE;
        } else {
            // Taxa capturada agora: o histórico do pagamento não muda com a cotação futura
            rate = request.rateToExpenseCurrency() != null
                    ? request.rateToExpenseCurrency()
                    : exchange.converterFor(userId).factor(request.currency(), expense.getCurrency());
        }
        payments.save(new ExpensePayment(userId, expenseId, request.paidOn(),
                Money.of(request.amount(), request.currency()), rate, request.note()));
        return detail(userId, expense, objectives.requireOwned(userId, expense.getObjectiveId()));
    }

    @Transactional
    public ExpenseDetailResponse deletePayment(UUID userId, UUID expenseId, UUID paymentId) {
        Expense expense = require(userId, expenseId);
        ExpensePayment payment = payments.findByIdAndUserIdAndExpenseId(paymentId, userId, expenseId)
                .orElseThrow(() -> new NotFoundException("Pagamento"));
        payments.delete(payment);
        payments.flush();
        return detail(userId, expense, objectives.requireOwned(userId, expense.getObjectiveId()));
    }

    // ------------------------------------------------------------------ apoio

    private ExpenseDetailResponse detail(UUID userId, Expense expense, Objective objective) {
        expenses.flush();
        ExpenseResponse response = mapper(userId, objective).toResponse(queries.one(expense));
        return new ExpenseDetailResponse(response, payments(userId, expense.getId()));
    }

    private Expense.Data toData(UUID userId, Objective objective, ExpenseRequest r) {
        requireCurrency(r.currency());
        ExpenseCategory category = categories.require(userId, r.categoryId());
        if (category.isArchived()) {
            throw new BusinessRuleException("Esta categoria está arquivada.", "categoryId");
        }
        if (r.cycleId() != null) {
            ExpenseCycle cycle = cycles.require(userId, r.cycleId());
            if (cycle.isArchived()) {
                throw new BusinessRuleException("Este ciclo está arquivado.", "cycleId");
            }
        }
        if (r.dueDate() != null && r.endDate() != null && r.endDate().isBefore(r.dueDate())) {
            throw new BusinessRuleException("A data final deve ser posterior à data do gasto.", "endDate");
        }
        int quantity = r.quantity() == null ? 1 : r.quantity();
        int occurrences = ExpenseCalculator.resolveOccurrences(r.recurrence(), r.occurrences(),
                objective.getHorizonMonths());
        return new Expense.Data(objective.getId(), r.categoryId(), r.cycleId(), r.name(), r.description(),
                r.currency(), r.unitPrice(), quantity, r.recurrence(), occurrences, Boolean.TRUE.equals(r.refundable()),
                r.required() == null || r.required(), r.dueDate(), r.endDate(), r.notes());
    }

    private Expense require(UUID userId, UUID id) {
        return expenses.findByIdAndUserId(id, userId).orElseThrow(() -> new NotFoundException("Gasto"));
    }

    private void requireCurrency(String currency) {
        if (!currencies.isSupported(currency)) {
            throw new BusinessRuleException("Moeda não suportada.", "currency");
        }
    }

    private static boolean matches(ComputedExpense c, Filter f, String q) {
        Expense e = c.expense();
        return (f.categoryId() == null || f.categoryId().equals(e.getCategoryId()))
                && (f.cycleId() == null || f.cycleId().equals(e.getCycleId()))
                && (f.status() == null || f.status() == c.status())
                && (f.required() == null || f.required() == e.isRequired())
                && (f.currency() == null || f.currency().equals(e.getCurrency()))
                && (f.from() == null || (e.getDueDate() != null && !e.getDueDate().isBefore(f.from())))
                && (f.to() == null || (e.getDueDate() != null && !e.getDueDate().isAfter(f.to())))
                && (q == null || e.getName().toLowerCase(Locale.ROOT).contains(q)
                        || (e.getNotes() != null && e.getNotes().toLowerCase(Locale.ROOT).contains(q)));
    }

    Mapper mapper(UUID userId, Objective objective) {
        return new Mapper(index(categories.entities(userId)), index(cycles.entities(userId)),
                exchange.converterFor(userId), objective.getCurrency());
    }

    private static <T extends CatalogItem> Map<UUID, T> index(List<T> items) {
        return items.stream().collect(Collectors.toMap(CatalogItem::getId, Function.identity()));
    }

    /** Converte gastos calculados em respostas, com o mesmo conversor para a lista inteira. */
    record Mapper(Map<UUID, ExpenseCategory> categories, Map<UUID, ExpenseCycle> cycles, CurrencyConverter converter,
            String objectiveCurrency) {

        ExpenseResponse toResponse(ComputedExpense c) {
            Expense e = c.expense();
            return new ExpenseResponse(e.getId(), e.getObjectiveId(), e.getName(), e.getDescription(),
                    ref(categories.get(e.getCategoryId())), e.getCycleId() == null ? null : ref(cycles.get(e.getCycleId())),
                    e.getCurrency(), e.getUnitPrice().setScale(2, Money.ROUNDING), e.getQuantity(), e.getRecurrence(),
                    e.getOccurrences(), MoneyResponse.of(c.total()),
                    MoneyResponse.of(converter.convert(c.total(), objectiveCurrency)), MoneyResponse.of(c.paid()),
                    MoneyResponse.of(c.remaining()), c.status(), e.isRequired(), e.isRefundable(), e.getDueDate(),
                    e.getEndDate(), e.getNotes());
        }

        private static ExpenseResponse.CatalogRef ref(CatalogItem item) {
            return item == null ? null
                    : new ExpenseResponse.CatalogRef(item.getId(), item.getName(), item.getColor(), item.getIcon());
        }
    }
}
