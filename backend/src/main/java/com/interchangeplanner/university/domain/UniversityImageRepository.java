package com.interchangeplanner.university.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UniversityImageRepository extends JpaRepository<UniversityImage, UUID> {

    List<UniversityImage> findByUserIdAndUniversityIdOrderByCreatedAtAsc(UUID userId, UUID universityId);

    Optional<UniversityImage> findByIdAndUserIdAndUniversityId(UUID id, UUID userId, UUID universityId);

    long countByUserIdAndUniversityId(UUID userId, UUID universityId);
}
