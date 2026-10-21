package com.interchangeplanner.objective.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ObjectiveRepository extends JpaRepository<Objective, UUID> {

    Optional<Objective> findByIdAndUserId(UUID id, UUID userId);

    Optional<Objective> findByUserIdAndPrimaryTrue(UUID userId);

    List<Objective> findByUserIdOrderByPrimaryDescTargetDateAsc(UUID userId);

    Optional<Objective> findFirstByUserIdAndIdNotOrderByCreatedAtDesc(UUID userId, UUID excludedId);

    boolean existsByUserId(UUID userId);

    /** Executado antes de marcar outro como principal (índice único parcial em is_primary). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Objective o set o.primary = false where o.userId = :userId and o.primary = true")
    int clearPrimary(@Param("userId") UUID userId);
}
