package com.interchangeplanner.applications.domain;

import com.interchangeplanner.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Algo que a universidade pediu durante o processo (documento, exame, entrevista, carta...).
 * Pendente enquanto {@code fulfilledOn} for nulo.
 */
@Entity
@Table(name = "application_request")
public class ApplicationRequest extends BaseEntity {

    public enum Type {
        DOCUMENT,
        EXAM,
        INTERVIEW,
        LETTER,
        RECOMMENDATION,
        TEST,
        PAYMENT,
        OTHER
    }

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "application_id", nullable = false, updatable = false)
    private UUID applicationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 16)
    private Type type;

    @Column(name = "description", nullable = false, length = 500)
    private String description;

    @Column(name = "requested_on", nullable = false)
    private LocalDate requestedOn;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "fulfilled_on")
    private LocalDate fulfilledOn;

    protected ApplicationRequest() {
    }

    public ApplicationRequest(UUID userId, UUID applicationId, Type type, String description, LocalDate requestedOn,
            LocalDate dueDate, LocalDate fulfilledOn) {
        this.userId = userId;
        this.applicationId = applicationId;
        update(type, description, requestedOn, dueDate, fulfilledOn);
    }

    public void update(Type type, String description, LocalDate requestedOn, LocalDate dueDate, LocalDate fulfilledOn) {
        this.type = type;
        this.description = description.strip();
        this.requestedOn = requestedOn;
        this.dueDate = dueDate;
        this.fulfilledOn = fulfilledOn;
    }

    public boolean isPending() {
        return fulfilledOn == null;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public Type getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getRequestedOn() {
        return requestedOn;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public LocalDate getFulfilledOn() {
        return fulfilledOn;
    }
}
