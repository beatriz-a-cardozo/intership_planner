package com.interchangeplanner.finance.ledger.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinanceCategoryRepository extends JpaRepository<FinanceCategory, UUID> {

    Optional<FinanceCategory> findByIdAndUserId(UUID id, UUID userId);

    List<FinanceCategory> findByUserIdOrderByKindAscSortOrderAscNameAsc(UUID userId);

    boolean existsByUserId(UUID userId);

    boolean existsByUserIdAndKindAndNameIgnoreCase(UUID userId, EntryKind kind, String name);

    boolean existsByUserIdAndKindAndNameIgnoreCaseAndIdNot(UUID userId, EntryKind kind, String name, UUID id);
}
