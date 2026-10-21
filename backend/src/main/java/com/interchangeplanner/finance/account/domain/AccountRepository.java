package com.interchangeplanner.finance.account.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    Optional<Account> findByIdAndUserId(UUID id, UUID userId);

    List<Account> findByUserIdOrderByArchivedAscNameAsc(UUID userId);

    List<Account> findByUserIdAndObjectiveIdAndArchivedFalse(UUID userId, UUID objectiveId);
}
