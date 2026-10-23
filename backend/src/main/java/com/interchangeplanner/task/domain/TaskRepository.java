package com.interchangeplanner.task.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, UUID> {

    Optional<Task> findByIdAndUserId(UUID id, UUID userId);

    List<Task> findByUserId(UUID userId);

    List<Task> findByUserIdAndDueDateBetween(UUID userId, LocalDate from, LocalDate to);
}
