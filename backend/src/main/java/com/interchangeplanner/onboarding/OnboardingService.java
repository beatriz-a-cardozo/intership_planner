package com.interchangeplanner.onboarding;

import com.interchangeplanner.finance.account.api.AccountRequest;
import com.interchangeplanner.finance.account.api.AccountResponse;
import com.interchangeplanner.finance.account.api.BalanceRequest;
import com.interchangeplanner.finance.account.application.AccountService;
import com.interchangeplanner.finance.account.domain.AccountType;
import com.interchangeplanner.objective.api.ObjectiveRequest;
import com.interchangeplanner.objective.api.ObjectiveResponse;
import com.interchangeplanner.objective.application.ObjectiveService;
import com.interchangeplanner.objective.domain.TargetMode;
import com.interchangeplanner.shared.error.BusinessRuleException;
import com.interchangeplanner.shared.error.ConflictException;
import com.interchangeplanner.user.application.UserService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cria, numa única transação, o objetivo principal e (se informado) a conta com o valor já guardado,
 * e marca o onboarding como concluído.
 *
 * <p>"Quanto já tem" vira uma conta vinculada ao objetivo, não um campo solto: assim entra no
 * "mobilizado" pela mesma regra de qualquer outro saldo e pode ser atualizado depois.</p>
 */
@Service
public class OnboardingService {

    static final String SAVINGS_ACCOUNT_NAME = "Reserva do intercâmbio";

    private final ObjectiveService objectives;
    private final AccountService accounts;
    private final UserService users;
    private final Clock clock;

    public OnboardingService(ObjectiveService objectives, AccountService accounts, UserService users, Clock clock) {
        this.objectives = objectives;
        this.accounts = accounts;
        this.users = users;
        this.clock = clock;
    }

    @Transactional
    public OnboardingResponse complete(UUID userId, OnboardingRequest r) {
        if (users.me(userId).onboardingCompleted()) {
            throw new ConflictException("O onboarding já foi concluído.", "objectiveName");
        }
        if (!r.targetDate().isAfter(LocalDate.now(clock))) {
            throw new BusinessRuleException("A data alvo precisa estar no futuro.", "targetDate");
        }
        TargetMode mode = r.targetAmount() != null ? TargetMode.MANUAL : TargetMode.DERIVED;
        ObjectiveResponse objective = objectives.create(userId, new ObjectiveRequest(r.objectiveName(), null,
                r.countryCode(), r.city(), null, r.targetDate(), r.currency(), mode, r.targetAmount(),
                r.plannedMonthlySaving(), null, null, null, null, null), true);

        if (r.currentSavings() != null && r.currentSavings().signum() > 0) {
            String currency = r.currentSavingsCurrency() != null ? r.currentSavingsCurrency() : r.currency();
            AccountResponse account = accounts.create(userId,
                    new AccountRequest(SAVINGS_ACCOUNT_NAME, currency, AccountType.SAVINGS, objective.id(), false));
            accounts.recordBalance(userId, account.id(), new BalanceRequest(LocalDate.now(clock), r.currentSavings()));
        }

        users.completeOnboarding(userId, clock.instant());
        return new OnboardingResponse(users.me(userId), objective);
    }
}
