package com.interchangeplanner.timeline.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TimelineEventRepository extends JpaRepository<TimelineEvent, UUID> {

    Optional<TimelineEvent> findByIdAndUserId(UUID id, UUID userId);

    List<TimelineEvent> findByUserId(UUID userId);

    /** Eventos que tocam o intervalo (um evento de vários dias aparece se qualquer dia cair nele). */
    @Query("""
            select e from TimelineEvent e
            where e.userId = :userId and e.startDate <= :to and coalesce(e.endDate, e.startDate) >= :from""")
    List<TimelineEvent> overlapping(@Param("userId") UUID userId, @Param("from") LocalDate from,
            @Param("to") LocalDate to);
}
