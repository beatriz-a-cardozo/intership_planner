package com.interchangeplanner.expense.application;

import com.interchangeplanner.expense.domain.Expense;
import com.interchangeplanner.expense.domain.ExpensePaymentRepository;
import com.interchangeplanner.expense.domain.ExpenseRepository;
import com.interchangeplanner.expense.domain.PaidSum;
import com.interchangeplanner.shared.money.Money;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Carrega gastos + total pago (uma query cada, sem N+1) e deriva total, restante e status. */
@Component
public class ExpenseQueries {

    private final ExpenseRepository expenses;
    private final ExpensePaymentRepository payments;

    public ExpenseQueries(ExpenseRepository expenses, ExpensePaymentRepository payments) {
        this.expenses = expenses;
        this.payments = payments;
    }

    @Transactional(readOnly = true)
    public List<ComputedExpense> forObjective(UUID userId, UUID objectiveId) {
        List<Expense> list = expenses.findByUserIdAndObjectiveId(userId, objectiveId);
        if (list.isEmpty()) {
            return List.of();
        }
        Map<UUID, BigDecimal> paid = payments.paidSums(userId, list.stream().map(Expense::getId).toList()).stream()
                .collect(Collectors.toMap(PaidSum::expenseId, PaidSum::paid));
        return list.stream().map(e -> compute(e, paid.getOrDefault(e.getId(), BigDecimal.ZERO))).toList();
    }

    @Transactional(readOnly = true)
    public ComputedExpense one(Expense expense) {
        List<PaidSum> sums = payments.paidSums(expense.getUserId(), List.of(expense.getId()));
        return compute(expense, sums.isEmpty() ? BigDecimal.ZERO : sums.getFirst().paid());
    }

    static ComputedExpense compute(Expense e, BigDecimal paidAmount) {
        Money total = ExpenseCalculator.total(e.unitPriceMoney(), e.getQuantity(), e.getOccurrences());
        Money paid = Money.of(paidAmount, e.getCurrency());
        return new ComputedExpense(e, total, paid, ExpenseCalculator.remaining(total, paid),
                ExpenseCalculator.status(e.isCancelled(), total, paid));
    }
}
