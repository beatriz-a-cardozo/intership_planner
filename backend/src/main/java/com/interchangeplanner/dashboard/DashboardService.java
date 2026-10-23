package com.interchangeplanner.dashboard;

import com.interchangeplanner.applications.application.ApplicationService;
import com.interchangeplanner.exchange.ExchangeProperties;
import com.interchangeplanner.exchange.api.ExchangeRateResponse;
import com.interchangeplanner.exchange.application.CurrencyConverter;
import com.interchangeplanner.exchange.application.ExchangeRateService;
import com.interchangeplanner.expense.api.ExpenseSummaryResponse;
import com.interchangeplanner.expense.application.ExpenseSummaryService;
import com.interchangeplanner.finance.account.application.AccountService;
import com.interchangeplanner.objective.api.ObjectiveResponse;
import com.interchangeplanner.objective.application.ObjectiveProgressService;
import com.interchangeplanner.objective.application.ObjectiveService;
import com.interchangeplanner.objective.domain.Objective;
import com.interchangeplanner.timeline.application.TimelineService;
import com.interchangeplanner.user.application.UserClock;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Orquestra os serviços dos módulos para a tela inicial (sem regras próprias). */
@Service
public class DashboardService {

    private final ObjectiveService objectives;
    private final ObjectiveProgressService progress;
    private final AccountService accounts;
    private final ExpenseSummaryService expenses;
    private final ExchangeRateService exchange;
    private final ExchangeProperties.Pair mainPair;
    private final ApplicationService applications;
    private final TimelineService timeline;
    private final UserClock userClock;

    public DashboardService(ObjectiveService objectives, ObjectiveProgressService progress, AccountService accounts,
            ExpenseSummaryService expenses, ExchangeRateService exchange, ExchangeProperties exchangeProperties,
            ApplicationService applications, TimelineService timeline, UserClock userClock) {
        this.objectives = objectives;
        this.progress = progress;
        this.accounts = accounts;
        this.expenses = expenses;
        this.exchange = exchange;
        this.mainPair = exchangeProperties.pairs().getFirst();
        this.applications = applications;
        this.timeline = timeline;
        this.userClock = userClock;
    }

    @Transactional(readOnly = true)
    public DashboardResponse dashboard(UUID userId, UUID objectiveId) {
        CurrencyConverter converter = exchange.converterFor(userId);
        Objective objective = objectives.resolve(userId, objectiveId).orElse(null);
        ExchangeRateResponse rate = ExchangeRateResponse.of(
                exchange.resolve(userId, mainPair.base(), mainPair.quote()),
                exchange.market(mainPair.base(), mainPair.quote()));
        ApplicationsOverview apps = ApplicationsOverview.of(applications.list(userId, null, null, false),
                userClock.today(userId));
        var upcoming = timeline.upcoming(userId, 6);
        if (objective == null) {
            return new DashboardResponse(null, null, accounts.summary(userId), null, rate, apps, upcoming);
        }
        return new DashboardResponse(
                ObjectiveResponse.from(objective),
                progress.progress(userId, objective, converter),
                accounts.summary(userId),
                expenses.summary(userId, objective, ExpenseSummaryResponse.GroupBy.CYCLE, converter),
                rate, apps, upcoming);
    }
}
