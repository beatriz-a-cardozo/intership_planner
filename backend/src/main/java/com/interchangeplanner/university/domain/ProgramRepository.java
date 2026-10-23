package com.interchangeplanner.university.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProgramRepository extends JpaRepository<Program, UUID> {

    Optional<Program> findByIdAndUserId(UUID id, UUID userId);

    List<Program> findByUserIdOrderByNameAsc(UUID userId);

    List<Program> findByUserIdAndUniversityIdOrderByNameAsc(UUID userId, UUID universityId);

    List<Program> findByUserIdAndIdIn(UUID userId, Collection<UUID> ids);
}
