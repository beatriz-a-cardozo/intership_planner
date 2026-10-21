package com.interchangeplanner.auth.domain;

import com.interchangeplanner.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Refresh token opaco. Só o hash SHA-256 é persistido; o valor real existe apenas no cookie.
 * Tokens emitidos a partir do mesmo login compartilham {@code familyId}: se um token já rotacionado
 * for reapresentado (possível roubo), a família inteira é revogada.
 */
@Entity
@Table(name = "refresh_token")
public class RefreshToken extends BaseEntity {

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "token_hash", nullable = false, length = 64, updatable = false)
    private String tokenHash;

    @Column(name = "family_id", nullable = false, updatable = false)
    private UUID familyId;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected RefreshToken() {
    }

    public static RefreshToken issue(UUID userId, String tokenHash, UUID familyId, Instant expiresAt) {
        RefreshToken token = new RefreshToken();
        token.userId = userId;
        token.tokenHash = tokenHash;
        token.familyId = familyId;
        token.expiresAt = expiresAt;
        return token;
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }

    public void revoke(Instant when) {
        if (revokedAt == null) {
            revokedAt = when;
        }
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getFamilyId() {
        return familyId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }
}
