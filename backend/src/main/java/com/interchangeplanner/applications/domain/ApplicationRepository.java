package com.interchangeplanner.applications.domain;

import com.interchangeplanner.university.domain.UniversityEnums.Term;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    Optional<Application> findByIdAndUserId(UUID id, UUID userId);

    List<Application> findByUserId(UUID userId);

    List<Application> findByUserIdAndProgramIdIn(UUID userId, Collection<UUID> programIds);

    Optional<Application> findByUserIdAndProgramIdAndIntakeYearAndTerm(UUID userId, UUID programId, int intakeYear,
            Term term);

    List<Application> findByUserIdAndProgramIdAndArchivedAtIsNull(UUID userId, UUID programId);
}
