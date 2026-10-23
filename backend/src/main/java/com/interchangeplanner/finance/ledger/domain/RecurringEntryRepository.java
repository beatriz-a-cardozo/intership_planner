package com.interchangeplanner.finance.ledger.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecurringEntryRepository extends JpaRepository<RecurringEntry, UUID> {

    Optional<RecurringEntry> findByIdAndUserId(UUID id, UUID userId);

    List<RecurringEntry> findByUserIdOrderByActiveDescNameAsc(UUID userId);

    /** Para o job diário: todas as recorrências ativas (de todos os usuários). */
    List<RecurringEntry> findByActiveTrue();

    boolean existsByCategoryId(UUID categoryId);
}
