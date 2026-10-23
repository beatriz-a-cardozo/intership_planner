package com.interchangeplanner.applications.domain;

import com.interchangeplanner.shared.domain.UuidV7;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/** Registro imutável de cada mudança de status (data informada pela pessoa + observação). */
@Entity
@Immutable
@Table(name = "application_status_history")
public class ApplicationStatusHistory {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id = UuidV7.generate();

    @Column(name = "application_id", nullable = false, updatable = false)
    private UUID applicationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ApplicationStatus status;

    @Column(name = "changed_on", nullable = false)
    private LocalDate changedOn;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ApplicationStatusHistory() {
    }

    public ApplicationStatusHistory(UUID applicationId, ApplicationStatus status, LocalDate changedOn, String note,
            Instant createdAt) {
        this.applicationId = applicationId;
        this.status = status;
        this.changedOn = changedOn;
        this.note = note == null || note.isBlank() ? null : note.strip();
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public LocalDate getChangedOn() {
        return changedOn;
    }

    public String getNote() {
        return note;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
