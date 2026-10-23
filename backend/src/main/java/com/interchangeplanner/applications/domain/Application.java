package com.interchangeplanner.applications.domain;

import com.interchangeplanner.shared.domain.BaseEntity;
import com.interchangeplanner.university.domain.UniversityEnums.Term;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Candidatura a um programa num ciclo concreto (ano + semestre). */
@Entity
@Table(name = "application")
public class Application extends BaseEntity {

    public enum Confidence {
        LOW,
        MEDIUM,
        HIGH
    }

    /** Campos editáveis (status muda só pelo fluxo com histórico). */
    public record Data(UUID objectiveId, int intakeYear, Term term, LocalDate submittedAt, LocalDate deadline,
            LocalDate responseMinDate, LocalDate responseLikelyDate, LocalDate responseMaxDate, Confidence confidence,
            String estimateBasis, LocalDate actualResponseDate, String notes) {
    }

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "objective_id")
    private UUID objectiveId;

    @Column(name = "program_id", nullable = false, updatable = false)
    private UUID programId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ApplicationStatus status;

    @Column(name = "intake_year", nullable = false)
    private short intakeYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "term", nullable = false, length = 10)
    private Term term;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Column(name = "submitted_at")
    private LocalDate submittedAt;

    @Column(name = "deadline")
    private LocalDate deadline;

    @Column(name = "response_min_date")
    private LocalDate responseMinDate;

    @Column(name = "response_likely_date")
    private LocalDate responseLikelyDate;

    @Column(name = "response_max_date")
    private LocalDate responseMaxDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "response_confidence", length = 6)
    private Confidence responseConfidence;

    @Column(name = "response_estimate_basis", length = 500)
    private String responseEstimateBasis;

    @Column(name = "actual_response_date")
    private LocalDate actualResponseDate;

    @Column(name = "notes", length = 2000)
    private String notes;

    protected Application() {
    }

    public Application(UUID userId, UUID programId, Data data) {
        this.userId = userId;
        this.programId = programId;
        this.status = ApplicationStatus.AWAITING_OPENING;
        update(data);
    }

    public void update(Data d) {
        this.objectiveId = d.objectiveId();
        this.intakeYear = (short) d.intakeYear();
        this.term = d.term();
        this.submittedAt = d.submittedAt();
        this.deadline = d.deadline();
        this.responseMinDate = d.responseMinDate();
        this.responseLikelyDate = d.responseLikelyDate();
        this.responseMaxDate = d.responseMaxDate();
        this.responseConfidence = d.confidence();
        this.responseEstimateBasis = d.estimateBasis() == null || d.estimateBasis().isBlank() ? null : d.estimateBasis().strip();
        this.actualResponseDate = d.actualResponseDate();
        this.notes = d.notes() == null || d.notes().isBlank() ? null : d.notes().strip();
    }

    public void changeStatus(ApplicationStatus status) {
        this.status = status;
    }

    public void archive(Instant when) {
        if (archivedAt == null) {
            archivedAt = when;
        }
    }

    public void unarchive() {
        archivedAt = null;
    }

    public boolean isArchived() {
        return archivedAt != null;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getObjectiveId() {
        return objectiveId;
    }

    public UUID getProgramId() {
        return programId;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public int getIntakeYear() {
        return intakeYear;
    }

    public Term getTerm() {
        return term;
    }

    public Instant getArchivedAt() {
        return archivedAt;
    }

    public LocalDate getSubmittedAt() {
        return submittedAt;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public LocalDate getResponseMinDate() {
        return responseMinDate;
    }

    public LocalDate getResponseLikelyDate() {
        return responseLikelyDate;
    }

    public LocalDate getResponseMaxDate() {
        return responseMaxDate;
    }

    public Confidence getResponseConfidence() {
        return responseConfidence;
    }

    public String getResponseEstimateBasis() {
        return responseEstimateBasis;
    }

    public LocalDate getActualResponseDate() {
        return actualResponseDate;
    }

    public String getNotes() {
        return notes;
    }
}
