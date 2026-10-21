package com.interchangeplanner.objective.application;

import com.interchangeplanner.currency.CurrencyService;
import com.interchangeplanner.objective.api.AdjustmentRequest;
import com.interchangeplanner.objective.api.AdjustmentResponse;
import com.interchangeplanner.objective.domain.ObjectiveAdjustment;
import com.interchangeplanner.objective.domain.ObjectiveAdjustmentRepository;
import com.interchangeplanner.shared.error.BusinessRuleException;
import com.interchangeplanner.shared.error.NotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdjustmentService {

    private final ObjectiveAdjustmentRepository adjustments;
    private final ObjectiveService objectives;
    private final CurrencyService currencies;

    public AdjustmentService(ObjectiveAdjustmentRepository adjustments, ObjectiveService objectives,
            CurrencyService currencies) {
        this.adjustments = adjustments;
        this.objectives = objectives;
        this.currencies = currencies;
    }

    @Transactional(readOnly = true)
    public List<AdjustmentResponse> list(UUID userId, UUID objectiveId) {
        objectives.requireOwned(userId, objectiveId);
        return entities(userId, objectiveId).stream().map(AdjustmentResponse::from).toList();
    }

    /** Para cálculos de outros módulos (o objetivo já foi validado pelo chamador). */
    @Transactional(readOnly = true)
    public List<ObjectiveAdjustment> entities(UUID userId, UUID objectiveId) {
        return adjustments.findByUserIdAndObjectiveIdOrderByCreatedAtAsc(userId, objectiveId);
    }

    @Transactional
    public AdjustmentResponse create(UUID userId, UUID objectiveId, AdjustmentRequest request) {
        objectives.requireOwned(userId, objectiveId);
        validate(request);
        return AdjustmentResponse.from(adjustments.save(new ObjectiveAdjustment(userId, objectiveId,
                request.label(), request.amount(), request.currency())));
    }

    @Transactional
    public AdjustmentResponse update(UUID userId, UUID objectiveId, UUID id, AdjustmentRequest request) {
        validate(request);
        ObjectiveAdjustment adjustment = find(userId, objectiveId, id);
        adjustment.update(request.label(), request.amount(), request.currency());
        return AdjustmentResponse.from(adjustment);
    }

    @Transactional
    public void delete(UUID userId, UUID objectiveId, UUID id) {
        adjustments.delete(find(userId, objectiveId, id));
    }

    private ObjectiveAdjustment find(UUID userId, UUID objectiveId, UUID id) {
        return adjustments.findByIdAndUserIdAndObjectiveId(id, userId, objectiveId)
                .orElseThrow(() -> new NotFoundException("Ajuste"));
    }

    private void validate(AdjustmentRequest request) {
        if (request.amount().signum() == 0) {
            throw new BusinessRuleException("O ajuste não pode ser zero.", "amount");
        }
        if (!currencies.isSupported(request.currency())) {
            throw new BusinessRuleException("Moeda não suportada.", "currency");
        }
    }
}
