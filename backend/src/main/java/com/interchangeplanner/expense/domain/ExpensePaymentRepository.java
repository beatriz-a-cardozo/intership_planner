package com.interchangeplanner.expense.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExpensePaymentRepository extends JpaRepository<ExpensePayment, UUID> {

    List<ExpensePayment> findByUserIdAndExpenseIdOrderByPaidOnAscCreatedAtAsc(UUID userId, UUID expenseId);

    Optional<ExpensePayment> findByIdAndUserIdAndExpenseId(UUID id, UUID userId, UUID expenseId);

    @Query("""
            select new com.interchangeplanner.expense.domain.PaidSum(
                p.expenseId, sum(p.amount * p.rateToExpenseCurrency))
            from ExpensePayment p
            where p.userId = :userId and p.expenseId in :expenseIds
            group by p.expenseId""")
    List<PaidSum> paidSums(@Param("userId") UUID userId, @Param("expenseIds") Collection<UUID> expenseIds);
}
