package com.interchangeplanner.user.api;

import com.interchangeplanner.user.domain.ThemePreference;

public record UserSettingsResponse(
        ThemePreference theme,
        String locale,
        String timezone,
        String primaryCurrency,
        String secondaryCurrency) {
}
