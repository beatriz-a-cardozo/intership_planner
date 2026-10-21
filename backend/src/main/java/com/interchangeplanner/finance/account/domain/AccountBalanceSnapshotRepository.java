package com.interchangeplanner.finance.account.domain;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountBalanceSnapshotRepository extends JpaRepository<AccountBalanceSnapshot, UUID> {

    Optional<AccountBalanceSnapshot> findByAccountIdAndAsOf(UUID accountId, LocalDate asOf);

    List<AccountBalanceSnapshot> findByAccountIdOrderByAsOfDesc(UUID accountId);

    /** Saldo mais recente de cada conta, numa única query (sem N+1). */
    @Query("""
            select s from AccountBalanceSnapshot s
            where s.accountId in :accountIds
              and s.asOf = (select max(s2.asOf) from AccountBalanceSnapshot s2 where s2.accountId = s.accountId)""")
    List<AccountBalanceSnapshot> findLatestFor(@Param("accountIds") Collection<UUID> accountIds);
}
