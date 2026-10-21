package com.interchangeplanner.objective.application;

import com.interchangeplanner.currency.CurrencyService;
import com.interchangeplanner.objective.api.ObjectiveRequest;
import com.interchangeplanner.objective.api.ObjectiveResponse;
import com.interchangeplanner.objective.domain.ExpenseScope;
import com.interchangeplanner.objective.domain.Objective;
import com.interchangeplanner.objective.domain.ObjectiveRepository;
import com.interchangeplanner.objective.domain.ObjectiveStatus;
import com.interchangeplanner.objective.domain.TargetMode;
import com.interchangeplanner.shared.error.BusinessRuleException;
import com.interchangeplanner.shared.error.NotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ObjectiveService {

    private final ObjectiveRepository objectives;
    private final CurrencyService currencies;

    public ObjectiveService(ObjectiveRepository objectives, CurrencyService currencies) {
        this.objectives = objectives;
        this.currencies = currencies;
    }

    @Transactional(readOnly = true)
    public List<ObjectiveResponse> list(UUID userId) {
        return objectives.findByUserIdOrderByPrimaryDescTargetDateAsc(userId).stream()
                .map(ObjectiveResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ObjectiveResponse get(UUID userId, UUID id) {
        return ObjectiveResponse.from(requireOwned(userId, id));
    }

    /** Para outros módulos: objetivo do usuário ou 404 (inexistente e "de outro usuário" são indistinguíveis). */
    @Transactional(readOnly = true)
    public Objective requireOwned(UUID userId, UUID id) {
        return objectives.findByIdAndUserId(id, userId).orElseThrow(() -> new NotFoundException("Objetivo"));
    }

    /** Objetivo informado ou, se nulo, o principal. */
    @Transactional(readOnly = true)
    public Optional<Objective> resolve(UUID userId, UUID objectiveIdOrNull) {
        if (objectiveIdOrNull != null) {
            return Optional.of(requireOwned(userId, objectiveIdOrNull));
        }
        return objectives.findByUserIdAndPrimaryTrue(userId);
    }

    @Transactional
    public ObjectiveResponse create(UUID userId, ObjectiveRequest request) {
        return create(userId, request, false);
    }

    /** @param forcePrimary torna principal mesmo que já exista outro (usado no onboarding) */
    @Transactional
    public ObjectiveResponse create(UUID userId, ObjectiveRequest request, boolean forcePrimary) {
        Objective objective = Objective.create(userId, toData(request));
        boolean first = !objectives.existsByUserId(userId);
        if (first || forcePrimary) {
            objectives.clearPrimary(userId);
            objective.markPrimary(true);
        }
        return ObjectiveResponse.from(objectives.save(objective));
    }

    @Transactional
    public ObjectiveResponse update(UUID userId, UUID id, ObjectiveRequest request) {
        Objective objective = requireOwned(userId, id);
        objective.update(toData(request));
        return ObjectiveResponse.from(objective);
    }

    @Transactional
    public ObjectiveResponse makePrimary(UUID userId, UUID id) {
        requireOwned(userId, id);
        objectives.clearPrimary(userId);
        // clearPrimary limpa o contexto de persistência: recarrega antes de alterar
        Objective objective = requireOwned(userId, id);
        objective.markPrimary(true);
        return ObjectiveResponse.from(objective);
    }

    /** Apaga o objetivo (gastos e ajustes em cascata; contas são desvinculadas). Promove outro a principal. */
    @Transactional
    public void delete(UUID userId, UUID id) {
        Objective objective = requireOwned(userId, id);
        boolean wasPrimary = objective.isPrimary();
        objectives.delete(objective);
        objectives.flush();
        if (wasPrimary) {
            objectives.findFirstByUserIdAndIdNotOrderByCreatedAtDesc(userId, id)
                    .ifPresent(next -> next.markPrimary(true));
        }
    }

    private Objective.Data toData(ObjectiveRequest r) {
        if (!currencies.isSupported(r.currency())) {
            throw new BusinessRuleException("Moeda não suportada.", "currency");
        }
        if (r.targetMode() == TargetMode.MANUAL && r.targetAmount() == null) {
            throw new BusinessRuleException("Informe o valor da meta.", "targetAmount");
        }
        if (r.startDate() != null && r.startDate().isAfter(r.targetDate())) {
            throw new BusinessRuleException("A data de início deve ser anterior à data alvo.", "startDate");
        }
        return new Objective.Data(r.name(), r.description(), r.countryCode(), r.city(), r.startDate(),
                r.targetDate(), r.currency(), r.targetMode(), r.targetAmount(), r.plannedMonthlySaving(),
                r.status() == null ? ObjectiveStatus.ACTIVE : r.status(), r.color(), r.icon(),
                r.horizonMonths() == null ? Objective.DEFAULT_HORIZON_MONTHS : r.horizonMonths(),
                r.expenseScope() == null ? ExpenseScope.REQUIRED_ONLY : r.expenseScope());
    }
}
