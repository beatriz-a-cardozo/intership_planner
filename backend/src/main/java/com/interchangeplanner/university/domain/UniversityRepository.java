package com.interchangeplanner.university.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UniversityRepository extends JpaRepository<University, UUID> {

    Optional<University> findByIdAndUserId(UUID id, UUID userId);

    List<University> findByUserIdOrderByNameAsc(UUID userId);
}
