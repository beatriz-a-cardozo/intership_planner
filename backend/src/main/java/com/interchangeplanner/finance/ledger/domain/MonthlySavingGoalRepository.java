package com.interchangeplanner.finance.ledger.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MonthlySavingGoalRepository extends JpaRepository<MonthlySavingGoal, UUID> {

    Optional<MonthlySavingGoal> findByUserIdAndYearMonth(UUID userId, LocalDate yearMonth);

    List<MonthlySavingGoal> findByUserIdAndYearMonthBetweenOrderByYearMonth(UUID userId, LocalDate from, LocalDate to);
}
