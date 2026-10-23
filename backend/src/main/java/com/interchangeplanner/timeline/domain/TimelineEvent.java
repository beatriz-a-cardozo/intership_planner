package com.interchangeplanner.timeline.domain;

import com.interchangeplanner.shared.domain.BaseEntity;
import com.interchangeplanner.shared.domain.Priority;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;

/** Evento criado pela pessoa (voo, prova, consulta no consulado...). Os automáticos não são gravados. */
@Entity
@Table(name = "timeline_event")
public class TimelineEvent extends BaseEntity {

    public record Data(String title, String description, LocalDate startDate, LocalDate endDate, EventType type,
            Priority priority, boolean done, UUID objectiveId) {
    }

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "objective_id")
    private UUID objectiveId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 16)
    private EventType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 10)
    private Priority priority;

    @Column(name = "done", nullable = false)
    private boolean done;

    protected TimelineEvent() {
    }

    public TimelineEvent(UUID userId, Data data) {
        this.userId = userId;
        update(data);
    }

    public void update(Data d) {
        this.title = d.title().strip();
        this.description = d.description() == null || d.description().isBlank() ? null : d.description().strip();
        this.startDate = d.startDate();
        this.endDate = d.endDate() == null || d.endDate().equals(d.startDate()) ? null : d.endDate();
        this.type = d.type() == null ? EventType.OTHER : d.type();
        this.priority = d.priority() == null ? Priority.MEDIUM : d.priority();
        this.done = d.done();
        this.objectiveId = d.objectiveId();
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    /** Último dia do evento (o próprio início se for de um dia). */
    public LocalDate lastDay() {
        return endDate == null ? startDate : endDate;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getObjectiveId() {
        return objectiveId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public EventType getType() {
        return type;
    }

    public Priority getPriority() {
        return priority;
    }

    public boolean isDone() {
        return done;
    }
}
