package com.interchangeplanner.expense.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseRepository extends JpaRepository<Expense, UUID> {

    Optional<Expense> findByIdAndUserId(UUID id, UUID userId);

    /** Todos os gastos do objetivo (a lista de um objetivo é pequena; filtros e status derivado são aplicados em memória). */
    List<Expense> findByUserIdAndObjectiveId(UUID userId, UUID objectiveId);

    boolean existsByCategoryId(UUID categoryId);

    boolean existsByCycleId(UUID cycleId);
}
