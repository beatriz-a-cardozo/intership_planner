package com.interchangeplanner.user.domain;

import com.interchangeplanner.shared.domain.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Preferências do usuário (1:1 com {@link AppUser}, chave = id do usuário). */
@Entity
@Table(name = "user_settings")
public class UserSettings extends AuditedEntity {

    public static final String DEFAULT_LOCALE = "pt-BR";
    public static final String DEFAULT_TIMEZONE = "America/Sao_Paulo";

    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "theme", nullable = false, length = 10)
    private ThemePreference theme;

    @Column(name = "locale", nullable = false, length = 10)
    private String locale;

    @Column(name = "timezone", nullable = false, length = 60)
    private String timezone;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "primary_currency", nullable = false, length = 3)
    private String primaryCurrency;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "secondary_currency", nullable = false, length = 3)
    private String secondaryCurrency;

    protected UserSettings() {
    }

    public static UserSettings defaultsFor(UUID userId) {
        UserSettings s = new UserSettings();
        s.userId = userId;
        s.theme = ThemePreference.SYSTEM;
        s.locale = DEFAULT_LOCALE;
        s.timezone = DEFAULT_TIMEZONE;
        s.primaryCurrency = "BRL";
        s.secondaryCurrency = "EUR";
        return s;
    }

    public void update(ThemePreference theme, String locale, String timezone, String primaryCurrency,
            String secondaryCurrency) {
        this.theme = theme;
        this.locale = locale;
        this.timezone = timezone;
        this.primaryCurrency = primaryCurrency;
        this.secondaryCurrency = secondaryCurrency;
    }

    public UUID getUserId() {
        return userId;
    }

    public ThemePreference getTheme() {
        return theme;
    }

    public String getLocale() {
        return locale;
    }

    public String getTimezone() {
        return timezone;
    }

    public String getPrimaryCurrency() {
        return primaryCurrency;
    }

    public String getSecondaryCurrency() {
        return secondaryCurrency;
    }
}
