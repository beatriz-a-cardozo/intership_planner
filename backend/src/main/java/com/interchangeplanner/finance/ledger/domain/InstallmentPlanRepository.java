package com.interchangeplanner.finance.ledger.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstallmentPlanRepository extends JpaRepository<InstallmentPlan, UUID> {

    Optional<InstallmentPlan> findByIdAndUserId(UUID id, UUID userId);

    List<InstallmentPlan> findByUserIdOrderByFirstDueDateDesc(UUID userId);

    boolean existsByCategoryId(UUID categoryId);
}
