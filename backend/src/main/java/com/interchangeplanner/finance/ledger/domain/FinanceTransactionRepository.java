package com.interchangeplanner.finance.ledger.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FinanceTransactionRepository extends JpaRepository<FinanceTransaction, UUID> {

    Optional<FinanceTransaction> findByIdAndUserId(UUID id, UUID userId);

    /** Lançamentos de um período (inclusive), em ordem de data. Uma query indexada por (user_id, occurred_on). */
    List<FinanceTransaction> findByUserIdAndOccurredOnBetweenOrderByOccurredOnAscCreatedAtAsc(UUID userId,
            LocalDate from, LocalDate to);

    List<FinanceTransaction> findByInstallmentPlanIdOrderByInstallmentNumberAsc(UUID installmentPlanId);

    List<FinanceTransaction> findByRecurringEntryIdAndOccurredOnBetween(UUID recurringEntryId, LocalDate from,
            LocalDate to);

    boolean existsByCategoryId(UUID categoryId);

    /** Remove as ocorrências futuras ainda não confirmadas de uma recorrência (antes de regenerar). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            delete from FinanceTransaction t
            where t.recurringEntryId = :entryId and t.status = com.interchangeplanner.finance.ledger.domain.TransactionStatus.PLANNED
              and t.occurredOn >= :from""")
    int deletePlannedFromRecurring(@Param("entryId") UUID entryId, @Param("from") LocalDate from);

    /** Remove as parcelas ainda não confirmadas de um parcelamento (antes de regenerar ou excluir). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            delete from FinanceTransaction t
            where t.installmentPlanId = :planId and t.status = com.interchangeplanner.finance.ledger.domain.TransactionStatus.PLANNED""")
    int deletePlannedFromInstallment(@Param("planId") UUID planId);

    /** Confirma ocorrências vencidas de recorrências com confirmação automática. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update FinanceTransaction t
            set t.status = com.interchangeplanner.finance.ledger.domain.TransactionStatus.CONFIRMED, t.confirmedAt = :now,
                t.updatedAt = :now, t.version = t.version + 1
            where t.status = com.interchangeplanner.finance.ledger.domain.TransactionStatus.PLANNED
              and t.occurredOn <= :today
              and t.recurringEntryId in (select r.id from RecurringEntry r where r.autoConfirm = true and r.active = true)""")
    int autoConfirmDue(@Param("today") LocalDate today, @Param("now") Instant now);
}
