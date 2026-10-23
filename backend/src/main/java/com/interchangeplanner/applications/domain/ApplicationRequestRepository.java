package com.interchangeplanner.applications.domain;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationRequestRepository extends JpaRepository<ApplicationRequest, UUID> {

    Optional<ApplicationRequest> findByIdAndUserIdAndApplicationId(UUID id, UUID userId, UUID applicationId);

    List<ApplicationRequest> findByUserIdAndApplicationIdOrderByRequestedOnAscCreatedAtAsc(UUID userId, UUID applicationId);

    List<ApplicationRequest> findByUserIdAndApplicationIdIn(UUID userId, Collection<UUID> applicationIds);

    List<ApplicationRequest> findByUserIdAndDueDateBetween(UUID userId, LocalDate from, LocalDate to);
}
