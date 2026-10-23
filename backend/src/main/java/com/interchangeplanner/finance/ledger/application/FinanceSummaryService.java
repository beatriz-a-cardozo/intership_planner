package com.interchangeplanner.finance.ledger.application;

import com.interchangeplanner.exchange.api.ExchangeRateResponse;
import com.interchangeplanner.exchange.application.CurrencyConverter;
import com.interchangeplanner.exchange.application.ExchangeRateService;
import com.interchangeplanner.finance.ledger.api.LedgerDtos.Amounts;
import com.interchangeplanner.finance.ledger.api.LedgerDtos.CategoryTotal;
import com.interchangeplanner.finance.ledger.api.LedgerDtos.CommitmentMonth;
import com.interchangeplanner.finance.ledger.api.LedgerDtos.CommitmentsResponse;
import com.interchangeplanner.finance.ledger.api.LedgerDtos.ExpenseAmounts;
import com.interchangeplanner.finance.ledger.api.LedgerDtos.MonthPoint;
import com.interchangeplanner.finance.ledger.api.LedgerDtos.MonthlySummaryResponse;
import com.interchangeplanner.finance.ledger.api.LedgerDtos.OverviewResponse;
import com.interchangeplanner.finance.ledger.domain.EntryKind;
import com.interchangeplanner.finance.ledger.domain.FinanceCategory;
import com.interchangeplanner.finance.ledger.domain.FinanceTransaction;
import com.interchangeplanner.finance.ledger.domain.FinanceTransactionRepository;
import com.interchangeplanner.finance.ledger.domain.MonthlySavingGoal;
import com.interchangeplanner.finance.ledger.domain.MonthlySavingGoalRepository;
import com.interchangeplanner.shared.error.BusinessRuleException;
import com.interchangeplanner.shared.money.Money;
import com.interchangeplanner.shared.money.MoneyResponse;
import com.interchangeplanner.user.application.UserService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Visão mensal, séries para gráficos e comprometido futuro. Tudo convertido para a moeda principal do
 * usuário com UMA cotação por resposta. Conta lançamentos previstos e confirmados; "realizado" = só confirmados.
 */
@Service
public class FinanceSummaryService {

    static final int MAX_OVERVIEW_MONTHS = 36;

    private final FinanceTransactionRepository transactions;
    private final MonthlySavingGoalRepository goals;
    private final FinanceCategoryService categories;
    private final LedgerSupport support;
    private final ExchangeRateService exchange;
    private final UserService users;
    private final Clock clock;

    public FinanceSummaryService(FinanceTransactionRepository transactions, MonthlySavingGoalRepository goals,
            FinanceCategoryService categories, LedgerSupport support, ExchangeRateService exchange, UserService users,
            Clock clock) {
        this.transactions = transactions;
        this.goals = goals;
        this.categories = categories;
        this.support = support;
        this.exchange = exchange;
        this.users = users;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public MonthlySummaryResponse monthly(UUID userId, YearMonth month) {
        String currency = users.settings(userId).primaryCurrency();
        CurrencyConverter converter = exchange.converterFor(userId);
        LocalDate today = today(userId);
        List<FinanceTransaction> txs = transactions.findByUserIdAndOccurredOnBetweenOrderByOccurredOnAscCreatedAtAsc(
                userId, month.atDay(1), month.atEndOfMonth());

        Totals t = Totals.of(txs, converter, currency);
        Map<UUID, FinanceCategory> cats = categories.index(userId);
        Money goal = goals.findByUserIdAndYearMonth(userId, month.atDay(1))
                .map(g -> converter.convert(g.money(), currency)).orElse(null);
        Money projected = t.income.minus(t.expenses);
        BigDecimal goalRatio = goal == null || goal.isZero() ? null
                : projected.amount().divide(goal.amount(), 4, RoundingMode.HALF_EVEN);
        int pending = (int) txs.stream().filter(x -> !x.isConfirmed() && x.getOccurredOn().isBefore(today)).count();

        Money committed = scheduledExpenses(userId, today.plusDays(1), today.plusMonths(12)).stream()
                .map(x -> converter.convert(x.money(), currency))
                .reduce(Money.zero(currency), Money::plus);

        return new MonthlySummaryResponse(month.toString(), currency,
                new Amounts(MoneyResponse.of(t.income), MoneyResponse.of(t.incomeConfirmed),
                        MoneyResponse.of(t.income.minus(t.incomeConfirmed))),
                new ExpenseAmounts(MoneyResponse.of(t.expenses), MoneyResponse.of(t.expensesConfirmed),
                        MoneyResponse.of(t.expenses.minus(t.expensesConfirmed)), MoneyResponse.of(t.oneOff),
                        MoneyResponse.of(t.installments), MoneyResponse.of(t.recurring)),
                MoneyResponse.of(projected), MoneyResponse.of(t.incomeConfirmed.minus(t.expensesConfirmed)),
                goal == null ? null : MoneyResponse.of(goal), goalRatio,
                byCategory(txs, EntryKind.EXPENSE, cats, converter, currency),
                byCategory(txs, EntryKind.INCOME, cats, converter, currency),
                support.toResponses(userId, txs), pending, MoneyResponse.of(committed),
                ExchangeRateResponse.of(converter));
    }

    @Transactional(readOnly = true)
    public OverviewResponse overview(UUID userId, YearMonth from, YearMonth to) {
        if (from.isAfter(to)) {
            throw new BusinessRuleException("O mês inicial deve ser anterior ao final.", "from");
        }
        if (from.plusMonths(MAX_OVERVIEW_MONTHS).isBefore(to)) {
            throw new BusinessRuleException("Período máximo de 36 meses.", "to");
        }
        String currency = users.settings(userId).primaryCurrency();
        CurrencyConverter converter = exchange.converterFor(userId);
        Map<YearMonth, List<FinanceTransaction>> byMonth = transactions
                .findByUserIdAndOccurredOnBetweenOrderByOccurredOnAscCreatedAtAsc(userId, from.atDay(1), to.atEndOfMonth())
                .stream().collect(Collectors.groupingBy(x -> YearMonth.from(x.getOccurredOn())));
        Map<YearMonth, MonthlySavingGoal> goalIndex = goals
                .findByUserIdAndYearMonthBetweenOrderByYearMonth(userId, from.atDay(1), to.atDay(1)).stream()
                .collect(Collectors.toMap(MonthlySavingGoal::month, Function.identity()));

        List<MonthPoint> points = new ArrayList<>();
        Money cumulative = Money.zero(currency);
        for (YearMonth m = from; !m.isAfter(to); m = m.plusMonths(1)) {
            Totals t = Totals.of(byMonth.getOrDefault(m, List.of()), converter, currency);
            Money surplus = t.income.minus(t.expenses);
            cumulative = cumulative.plus(surplus);
            MonthlySavingGoal g = goalIndex.get(m);
            points.add(new MonthPoint(m.toString(), MoneyResponse.of(t.income), MoneyResponse.of(t.expenses),
                    MoneyResponse.of(surplus), MoneyResponse.of(cumulative),
                    g == null ? null : MoneyResponse.of(converter.convert(g.money(), currency))));
        }
        return new OverviewResponse(currency, points);
    }

    /** Parcelas e recorrências de despesa previstas, mês a mês, + próximos vencimentos (45 dias). */
    @Transactional(readOnly = true)
    public CommitmentsResponse commitments(UUID userId, int months) {
        if (months < 1 || months > MAX_OVERVIEW_MONTHS) {
            throw new BusinessRuleException("Entre 1 e 36 meses.", "months");
        }
        String currency = users.settings(userId).primaryCurrency();
        CurrencyConverter converter = exchange.converterFor(userId);
        LocalDate today = today(userId);
        YearMonth first = YearMonth.from(today);
        YearMonth last = first.plusMonths(months - 1L);
        List<FinanceTransaction> scheduled = scheduledExpenses(userId, today, last.atEndOfMonth()).stream()
                .filter(x -> !x.isConfirmed())
                .toList();

        Map<YearMonth, Money[]> acc = new LinkedHashMap<>();
        for (YearMonth m = first; !m.isAfter(last); m = m.plusMonths(1)) {
            acc.put(m, new Money[] {Money.zero(currency), Money.zero(currency)});
        }
        Money total = Money.zero(currency);
        for (FinanceTransaction x : scheduled) {
            Money v = converter.convert(x.money(), currency);
            Money[] slot = acc.get(YearMonth.from(x.getOccurredOn()));
            if (slot == null) {
                continue;
            }
            int i = x.getInstallmentPlanId() != null ? 0 : 1;
            slot[i] = slot[i].plus(v);
            total = total.plus(v);
        }
        List<CommitmentMonth> monthsList = acc.entrySet().stream()
                .map(e -> new CommitmentMonth(e.getKey().toString(), MoneyResponse.of(e.getValue()[0]),
                        MoneyResponse.of(e.getValue()[1]), MoneyResponse.of(e.getValue()[0].plus(e.getValue()[1]))))
                .toList();
        List<FinanceTransaction> upcoming = scheduled.stream()
                .filter(x -> !x.getOccurredOn().isAfter(today.plusDays(45)))
                .sorted(Comparator.comparing(FinanceTransaction::getOccurredOn))
                .limit(20)
                .toList();
        return new CommitmentsResponse(currency, MoneyResponse.of(total), monthsList,
                support.toResponses(userId, upcoming));
    }

    private List<FinanceTransaction> scheduledExpenses(UUID userId, LocalDate from, LocalDate to) {
        return transactions.findByUserIdAndOccurredOnBetweenOrderByOccurredOnAscCreatedAtAsc(userId, from, to).stream()
                .filter(x -> x.getKind() == EntryKind.EXPENSE && x.isScheduled())
                .toList();
    }

    private List<CategoryTotal> byCategory(List<FinanceTransaction> txs, EntryKind kind, Map<UUID, FinanceCategory> cats,
            CurrencyConverter converter, String currency) {
        Map<UUID, Money> totals = new LinkedHashMap<>();
        Map<UUID, Integer> counts = new LinkedHashMap<>();
        for (FinanceTransaction x : txs) {
            if (x.getKind() != kind) {
                continue;
            }
            totals.merge(x.getCategoryId(), converter.convert(x.money(), currency), Money::plus);
            counts.merge(x.getCategoryId(), 1, Integer::sum);
        }
        return totals.entrySet().stream()
                .sorted(Map.Entry.<UUID, Money>comparingByValue().reversed())
                .map(e -> new CategoryTotal(FinanceCategoryService.ref(cats.get(e.getKey())),
                        MoneyResponse.of(e.getValue()), counts.get(e.getKey())))
                .toList();
    }

    LocalDate today(UUID userId) {
        return LocalDate.now(clock.withZone(ZoneId.of(users.settings(userId).timezone())));
    }

    /** Somatórios de um conjunto de lançamentos, convertidos. */
    static final class Totals {
        Money income;
        Money incomeConfirmed;
        Money expenses;
        Money expensesConfirmed;
        Money oneOff;
        Money installments;
        Money recurring;

        static Totals of(List<FinanceTransaction> txs, CurrencyConverter converter, String currency) {
            Totals t = new Totals();
            Money zero = Money.zero(currency);
            t.income = t.incomeConfirmed = t.expenses = t.expensesConfirmed = zero;
            t.oneOff = t.installments = t.recurring = zero;
            for (FinanceTransaction x : txs) {
                Money v = converter.convert(x.money(), currency);
                if (x.getKind() == EntryKind.INCOME) {
                    t.income = t.income.plus(v);
                    if (x.isConfirmed()) {
                        t.incomeConfirmed = t.incomeConfirmed.plus(v);
                    }
                } else {
                    t.expenses = t.expenses.plus(v);
                    if (x.isConfirmed()) {
                        t.expensesConfirmed = t.expensesConfirmed.plus(v);
                    }
                    if (x.getInstallmentPlanId() != null) {
                        t.installments = t.installments.plus(v);
                    } else if (x.getRecurringEntryId() != null) {
                        t.recurring = t.recurring.plus(v);
                    } else {
                        t.oneOff = t.oneOff.plus(v);
                    }
                }
            }
            return t;
        }
    }
}
