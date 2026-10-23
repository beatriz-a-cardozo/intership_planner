package com.interchangeplanner.finance.ledger.application;

import com.interchangeplanner.finance.ledger.api.LedgerDtos.SavingGoalRequest;
import com.interchangeplanner.finance.ledger.api.LedgerDtos.SavingGoalResponse;
import com.interchangeplanner.finance.ledger.domain.MonthlySavingGoal;
import com.interchangeplanner.finance.ledger.domain.MonthlySavingGoalRepository;
import com.interchangeplanner.shared.money.Money;
import com.interchangeplanner.shared.money.MoneyResponse;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SavingGoalService {

    private final MonthlySavingGoalRepository goals;
    private final LedgerSupport support;

    public SavingGoalService(MonthlySavingGoalRepository goals, LedgerSupport support) {
        this.goals = goals;
        this.support = support;
    }

    @Transactional(readOnly = true)
    public List<SavingGoalResponse> list(UUID userId, YearMonth from, YearMonth to) {
        return goals.findByUserIdAndYearMonthBetweenOrderByYearMonth(userId, from.atDay(1), to.atDay(1)).stream()
                .map(SavingGoalService::toResponse)
                .toList();
    }

    /** Define (ou substitui) a meta do mês. */
    @Transactional
    public SavingGoalResponse set(UUID userId, YearMonth month, SavingGoalRequest r) {
        support.requireCurrency(r.currency());
        Money amount = Money.of(r.amount(), r.currency());
        MonthlySavingGoal goal = goals.findByUserIdAndYearMonth(userId, month.atDay(1))
                .map(g -> {
                    g.change(amount);
                    return g;
                })
                .orElseGet(() -> goals.save(new MonthlySavingGoal(userId, month, amount)));
        return toResponse(goal);
    }

    @Transactional
    public void delete(UUID userId, YearMonth month) {
        goals.findByUserIdAndYearMonth(userId, month.atDay(1)).ifPresent(goals::delete);
    }

    private static SavingGoalResponse toResponse(MonthlySavingGoal g) {
        return new SavingGoalResponse(g.month().toString(), MoneyResponse.of(g.money()));
    }
}
