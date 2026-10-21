package com.interchangeplanner.user.api;

import java.util.UUID;

public record MeResponse(
        UUID id,
        String email,
        String displayName,
        boolean onboardingCompleted,
        UserSettingsResponse settings) {
}
