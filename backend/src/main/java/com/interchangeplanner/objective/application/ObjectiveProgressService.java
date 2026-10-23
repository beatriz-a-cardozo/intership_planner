package com.interchangeplanner.objective.application;

import com.interchangeplanner.exchange.api.ExchangeRateResponse;
import com.interchangeplanner.exchange.application.CurrencyConverter;
import com.interchangeplanner.exchange.application.ExchangeRateService;
import com.interchangeplanner.expense.application.ExpenseSummaryService;
import com.interchangeplanner.expense.application.ExpenseSummaryService.ObjectiveExpenseTotals;
import com.interchangeplanner.finance.account.application.AccountService;
import com.interchangeplanner.objective.api.ObjectiveProgressResponse;
import com.interchangeplanner.objective.api.ObjectiveProgressResponse.ScopeGoal;
import com.interchangeplanner.objective.domain.ExpenseScope;
import com.interchangeplanner.objective.domain.Objective;
import com.interchangeplanner.objective.domain.TargetMode;
import com.interchangeplanner.shared.money.Money;
import com.interchangeplanner.shared.money.MoneyResponse;
import com.interchangeplanner.user.application.UserService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reúne os insumos (gastos, ajustes, contas, cotação) e aplica o {@link ObjectiveProgressCalculator}. */
@Service
public class ObjectiveProgressService {

    private final ObjectiveService objectives;
    private final ExpenseSummaryService expenses;
    private final AccountService accounts;
    private final ExchangeRateService exchange;
    private final UserService users;
    private final Clock clock;

    public ObjectiveProgressService(ObjectiveService objectives, ExpenseSummaryService expenses,
            AccountService accounts, ExchangeRateService exchange,
            UserService users, Clock clock) {
        this.objectives = objectives;
        this.expenses = expenses;
        this.accounts = accounts;
        this.exchange = exchange;
        this.users = users;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ObjectiveProgressResponse progress(UUID userId, UUID objectiveId) {
        return progress(userId, objectives.requireOwned(userId, objectiveId), exchange.converterFor(userId));
    }

    @Transactional(readOnly = true)
    public ObjectiveProgressResponse progress(UUID userId, Objective o, CurrencyConverter converter) {
        String currency = o.getCurrency();
        ObjectiveExpenseTotals totals = expenses.totalsFor(userId, o, converter);
        Money accountsBalance = accounts.balanceForObjective(userId, o.getId(), currency, converter);
        Money planned = o.getPlannedMonthlySaving() == null ? null : Money.of(o.getPlannedMonthlySaving(), currency);

        LocalDate today = today(userId);
        ObjectiveProgressCalculator.Result r = ObjectiveProgressCalculator.calculate(
                new ObjectiveProgressCalculator.Inputs(currency, o.getTargetMode(),
                        o.getTargetAmount() == null ? null : Money.of(o.getTargetAmount(), currency),
                        totals.plannedInScope(), totals.adjustments(), totals.paidInScope(), accountsBalance,
                        o.getTargetDate(), today, planned));
        List<ScopeGoal> scopeGoals = List.of(
                scopeGoal(ExpenseScope.REQUIRED_ONLY, totals.plannedRequired(), totals.paidRequired(), totals, o,
                        accountsBalance, today),
                scopeGoal(ExpenseScope.ALL, totals.plannedAll(), totals.paidAll(), totals, o, accountsBalance, today));

        return new ObjectiveProgressResponse(o.getId(), currency, o.getTargetMode(), o.getExpenseScope(),
                MoneyResponse.of(totals.plannedInScope()), MoneyResponse.of(totals.adjustments()),
                MoneyResponse.of(r.plannedNeed()), MoneyResponse.of(r.target()), MoneyResponse.of(totals.paidInScope()),
                MoneyResponse.of(accountsBalance), MoneyResponse.of(r.mobilized()), MoneyResponse.of(r.remaining()),
                r.ratio(), r.percentComplete(), o.getTargetDate(), r.monthsRemaining(),
                MoneyResponse.of(r.monthlySavingNeeded()), planned == null ? null : MoneyResponse.of(planned),
                r.onTrack(), r.overdue(), ExchangeRateResponse.of(converter), scopeGoals);
    }

    /** Meta derivada de um escopo (mesma regra da meta principal, sempre no modo DERIVED). */
    private static ScopeGoal scopeGoal(ExpenseScope scope, Money plannedExpenses, Money paid, ObjectiveExpenseTotals totals,
            Objective o, Money accountsBalance, LocalDate today) {
        ObjectiveProgressCalculator.Result r = ObjectiveProgressCalculator.calculate(
                new ObjectiveProgressCalculator.Inputs(o.getCurrency(), TargetMode.DERIVED, null, plannedExpenses,
                        totals.adjustments(), paid, accountsBalance, o.getTargetDate(), today, null));
        return new ScopeGoal(scope, MoneyResponse.of(plannedExpenses), MoneyResponse.of(r.target()),
                MoneyResponse.of(r.mobilized()), MoneyResponse.of(r.remaining()), r.ratio(), r.percentComplete(),
                MoneyResponse.of(r.monthlySavingNeeded()), scope == o.getExpenseScope());
    }

    /** "Hoje" no fuso do usuário (o mês corrente depende dele). */
    private LocalDate today(UUID userId) {
        return LocalDate.now(clock.withZone(ZoneId.of(users.settings(userId).timezone())));
    }
}
