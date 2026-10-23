package com.interchangeplanner.task.domain;

import com.interchangeplanner.shared.domain.BaseEntity;
import com.interchangeplanner.shared.domain.Priority;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Tarefa do planejamento, com vínculos opcionais (objetivo, universidade, gasto, candidatura).
 * {@code completedAt} existe exatamente quando o status é DONE (CHECK no banco).
 */
@Entity
@Table(name = "task")
public class Task extends BaseEntity {

    public record Data(String title, String description, Priority priority, LocalDate dueDate, String category,
            UUID objectiveId, UUID universityId, UUID expenseId, UUID applicationId) {
    }

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 10)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 12)
    private TaskStatus status = TaskStatus.TODO;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "category", length = 40)
    private String category;

    @Column(name = "objective_id")
    private UUID objectiveId;

    @Column(name = "university_id")
    private UUID universityId;

    @Column(name = "expense_id")
    private UUID expenseId;

    @Column(name = "application_id")
    private UUID applicationId;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected Task() {
    }

    public Task(UUID userId, Data data) {
        this.userId = userId;
        update(data);
    }

    public void update(Data d) {
        this.title = d.title().strip();
        this.description = blankToNull(d.description());
        this.priority = d.priority() == null ? Priority.MEDIUM : d.priority();
        this.dueDate = d.dueDate();
        this.category = blankToNull(d.category());
        this.objectiveId = d.objectiveId();
        this.universityId = d.universityId();
        this.expenseId = d.expenseId();
        this.applicationId = d.applicationId();
    }

    public void changeStatus(TaskStatus status, Instant now) {
        if (status == this.status) {
            return;
        }
        this.status = status;
        this.completedAt = status == TaskStatus.DONE ? now : null;
    }

    public boolean isDone() {
        return status == TaskStatus.DONE;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.strip();
    }

    public UUID getUserId() {
        return userId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Priority getPriority() {
        return priority;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public String getCategory() {
        return category;
    }

    public UUID getObjectiveId() {
        return objectiveId;
    }

    public UUID getUniversityId() {
        return universityId;
    }

    public UUID getExpenseId() {
        return expenseId;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}
