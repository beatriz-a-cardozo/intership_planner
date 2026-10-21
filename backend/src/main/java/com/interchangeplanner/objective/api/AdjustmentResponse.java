package com.interchangeplanner.objective.api;

import com.interchangeplanner.objective.domain.ObjectiveAdjustment;
import com.interchangeplanner.shared.money.MoneyResponse;
import java.util.UUID;

public record AdjustmentResponse(UUID id, UUID objectiveId, String label, MoneyResponse amount) {

    public static AdjustmentResponse from(ObjectiveAdjustment a) {
        return new AdjustmentResponse(a.getId(), a.getObjectiveId(), a.getLabel(), MoneyResponse.of(a.money()));
    }
}
