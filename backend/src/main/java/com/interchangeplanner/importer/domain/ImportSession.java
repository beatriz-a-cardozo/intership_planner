package com.interchangeplanner.importer.domain;

import com.interchangeplanner.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Prévia de uma importação, guardada até ser importada ou expirar. */
@Entity
@Table(name = "import_session")
public class ImportSession extends BaseEntity {

    public enum Status {
        PREVIEW,
        COMMITTED
    }

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "file_name", nullable = false, updatable = false, length = 200)
    private String fileName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 12)
    private Status status = Status.PREVIEW;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, updatable = false)
    private String payload;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "summary")
    private String summary;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "committed_at")
    private Instant committedAt;

    protected ImportSession() {
    }

    public ImportSession(UUID userId, String fileName, String payload, Instant expiresAt) {
        this.userId = userId;
        this.fileName = fileName;
        this.payload = payload;
        this.expiresAt = expiresAt;
    }

    public void commit(String summary, Instant now) {
        this.status = Status.COMMITTED;
        this.summary = summary;
        this.committedAt = now;
    }

    public boolean isExpired(Instant now) {
        return status == Status.PREVIEW && now.isAfter(expiresAt);
    }

    public UUID getUserId() {
        return userId;
    }

    public String getFileName() {
        return fileName;
    }

    public Status getStatus() {
        return status;
    }

    public String getPayload() {
        return payload;
    }

    public String getSummary() {
        return summary;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCommittedAt() {
        return committedAt;
    }
}
