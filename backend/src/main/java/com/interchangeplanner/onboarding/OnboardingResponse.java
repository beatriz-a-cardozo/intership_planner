package com.interchangeplanner.onboarding;

import com.interchangeplanner.objective.api.ObjectiveResponse;
import com.interchangeplanner.user.api.MeResponse;

public record OnboardingResponse(MeResponse user, ObjectiveResponse objective) {
}
