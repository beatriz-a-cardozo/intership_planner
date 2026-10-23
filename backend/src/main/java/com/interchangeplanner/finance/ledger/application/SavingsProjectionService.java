package com.interchangeplanner.finance.ledger.application;

import com.interchangeplanner.exchange.api.ExchangeRateResponse;
import com.interchangeplanner.exchange.application.CurrencyConverter;
import com.interchangeplanner.exchange.application.ExchangeRateService;
import com.interchangeplanner.finance.ledger.api.LedgerDtos.ProjectionMonth;
import com.interchangeplanner.finance.ledger.api.LedgerDtos.ProjectionResponse;
import com.interchangeplanner.finance.ledger.domain.EntryKind;
import com.interchangeplanner.finance.ledger.domain.FinanceTransaction;
import com.interchangeplanner.finance.ledger.domain.FinanceTransactionRepository;
import com.interchangeplanner.objective.api.ObjectiveProgressResponse;
import com.interchangeplanner.objective.application.ObjectiveProgressService;
import com.interchangeplanner.objective.application.ObjectiveService;
import com.interchangeplanner.objective.domain.Objective;
import com.interchangeplanner.shared.money.Money;
import com.interchangeplanner.shared.money.MoneyResponse;
import com.interchangeplanner.user.application.UserService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Monta os insumos da {@link SavingsProjectionCalculator} a partir do controle financeiro e do progresso do objetivo. */
@Service
public class SavingsProjectionService {

    /** Meses fechados usados na média das sobras avulsas. */
    static final int BASIS_MONTHS = 3;
    /** Limite da projeção (alinhado à janela de recorrências materializadas). */
    static final int MAX_MONTHS = 36;

    private final ObjectiveService objectives;
    private final ObjectiveProgressService progress;
    private final FinanceTransactionRepository transactions;
    private final ExchangeRateService exchange;
    private final UserService users;
    private final Clock clock;

    public SavingsProjectionService(ObjectiveService objectives, ObjectiveProgressService progress,
            FinanceTransactionRepository transactions, ExchangeRateService exchange, UserService users, Clock clock) {
        this.objectives = objectives;
        this.progress = progress;
        this.transactions = transactions;
        this.exchange = exchange;
        this.users = users;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ProjectionResponse project(UUID userId, UUID objectiveId) {
        Objective objective = objectives.requireOwned(userId, objectiveId);
        CurrencyConverter converter = exchange.converterFor(userId);
        ObjectiveProgressResponse p = progress.progress(userId, objective, converter);
        String currency = objective.getCurrency();
        YearMonth current = YearMonth.from(LocalDate.now(clock.withZone(ZoneId.of(users.settings(userId).timezone()))));

        // média das sobras avulsas confirmadas nos meses fechados que têm algum lançamento confirmado
        YearMonth basisStart = current.minusMonths(BASIS_MONTHS);
        Map<YearMonth, List<FinanceTransaction>> past = transactions
                .findByUserIdAndOccurredOnBetweenOrderByOccurredOnAscCreatedAtAsc(userId, basisStart.atDay(1),
                        current.minusMonths(1).atEndOfMonth())
                .stream().filter(FinanceTransaction::isConfirmed)
                .collect(Collectors.groupingBy(t -> YearMonth.from(t.getOccurredOn())));
        List<YearMonth> basis = past.keySet().stream().sorted().toList();
        Money oneOffNet = Money.zero(currency);
        for (List<FinanceTransaction> list : past.values()) {
            for (FinanceTransaction t : list) {
                if (t.isScheduled()) {
                    continue;
                }
                Money v = converter.convert(t.money(), currency);
                oneOffNet = t.getKind() == EntryKind.INCOME ? oneOffNet.plus(v) : oneOffNet.minus(v);
            }
        }
        Money baseline = basis.isEmpty() ? Money.zero(currency) : oneOffNet.dividedBy(basis.size());

        // receitas e despesas programadas (parcelas e recorrências) nos meses futuros
        Map<YearMonth, Money> plannedIncome = new HashMap<>();
        Map<YearMonth, Money> plannedExpenses = new HashMap<>();
        transactions.findByUserIdAndOccurredOnBetweenOrderByOccurredOnAscCreatedAtAsc(userId,
                        current.plusMonths(1).atDay(1), current.plusMonths(MAX_MONTHS).atEndOfMonth())
                .stream().filter(FinanceTransaction::isScheduled)
                .forEach(t -> (t.getKind() == EntryKind.INCOME ? plannedIncome : plannedExpenses)
                        .merge(YearMonth.from(t.getOccurredOn()), converter.convert(t.money(), currency), Money::plus));

        Money target = Money.of(p.target().amount(), currency);
        Money mobilized = Money.of(p.mobilized().amount(), currency);
        SavingsProjectionCalculator.Result r = SavingsProjectionCalculator.project(new SavingsProjectionCalculator.Inputs(
                target, mobilized, current, YearMonth.from(objective.getTargetDate()), baseline, plannedIncome,
                plannedExpenses, MAX_MONTHS));

        return new ProjectionResponse(objective.getId(), currency, MoneyResponse.of(target), MoneyResponse.of(mobilized),
                MoneyResponse.of(baseline), basis.stream().map(YearMonth::toString).toList(), !basis.isEmpty(),
                r.months().stream().map(m -> new ProjectionMonth(m.month().toString(), MoneyResponse.of(m.plannedIncome()),
                        MoneyResponse.of(m.plannedExpenses()), MoneyResponse.of(m.expectedSaving()),
                        MoneyResponse.of(m.cumulative()))).toList(),
                r.reachMonth() == null ? null : r.reachMonth().toString(), r.reachesByTarget(),
                MoneyResponse.of(r.deficitAtTarget()), ExchangeRateResponse.of(converter));
    }
}
