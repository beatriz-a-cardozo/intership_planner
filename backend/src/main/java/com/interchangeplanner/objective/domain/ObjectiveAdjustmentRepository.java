package com.interchangeplanner.objective.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ObjectiveAdjustmentRepository extends JpaRepository<ObjectiveAdjustment, UUID> {

    List<ObjectiveAdjustment> findByUserIdAndObjectiveIdOrderByCreatedAtAsc(UUID userId, UUID objectiveId);

    Optional<ObjectiveAdjustment> findByIdAndUserIdAndObjectiveId(UUID id, UUID userId, UUID objectiveId);
}
