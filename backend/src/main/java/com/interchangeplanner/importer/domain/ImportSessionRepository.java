package com.interchangeplanner.importer.domain;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ImportSessionRepository extends JpaRepository<ImportSession, UUID> {

    Optional<ImportSession> findByIdAndUserId(UUID id, UUID userId);

    /** Remove prévias vencidas da pessoa (as importadas ficam como histórico). */
    @Modifying
    @Query("delete from ImportSession s where s.userId = :userId and s.status = 'PREVIEW' and s.expiresAt < :now")
    int deleteExpired(@Param("userId") UUID userId, @Param("now") Instant now);
}
