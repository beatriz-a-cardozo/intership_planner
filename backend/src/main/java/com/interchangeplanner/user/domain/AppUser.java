package com.interchangeplanner.user.domain;

import com.interchangeplanner.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

@Entity
@Table(name = "app_user")
public class AppUser extends BaseEntity {

    @Column(name = "email", nullable = false, length = 254)
    private String email;

    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = 80)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", nullable = false, length = 20)
    private AuthProvider authProvider;

    @Column(name = "onboarding_completed_at")
    private Instant onboardingCompletedAt;

    protected AppUser() {
    }

    public static AppUser local(String email, String passwordHash, String displayName) {
        AppUser user = new AppUser();
        user.email = normalizeEmail(email);
        user.passwordHash = Objects.requireNonNull(passwordHash);
        user.displayName = displayName.strip();
        user.authProvider = AuthProvider.LOCAL;
        return user;
    }

    /** Regra única de normalização de e-mail (também aplicada nas buscas). */
    public static String normalizeEmail(String email) {
        return email == null ? null : email.strip().toLowerCase(Locale.ROOT);
    }

    public void completeOnboarding(Instant when) {
        this.onboardingCompletedAt = when;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public AuthProvider getAuthProvider() {
        return authProvider;
    }

    public Instant getOnboardingCompletedAt() {
        return onboardingCompletedAt;
    }

    public boolean isOnboardingCompleted() {
        return onboardingCompletedAt != null;
    }
}
