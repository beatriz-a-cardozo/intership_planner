package com.interchangeplanner.finance.ledger.application;

import com.interchangeplanner.currency.CurrencyService;
import com.interchangeplanner.finance.account.domain.AccountRepository;
import com.interchangeplanner.finance.ledger.api.LedgerDtos.TransactionResponse;
import com.interchangeplanner.finance.ledger.api.LedgerDtos.TransactionSource;
import com.interchangeplanner.finance.ledger.domain.FinanceCategory;
import com.interchangeplanner.finance.ledger.domain.FinanceTransaction;
import com.interchangeplanner.finance.ledger.domain.InstallmentPlan;
import com.interchangeplanner.finance.ledger.domain.InstallmentPlanRepository;
import com.interchangeplanner.finance.ledger.domain.RecurringEntry;
import com.interchangeplanner.finance.ledger.domain.RecurringEntryRepository;
import com.interchangeplanner.shared.error.BusinessRuleException;
import com.interchangeplanner.shared.error.NotFoundException;
import com.interchangeplanner.shared.money.MoneyResponse;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Validações e mapeamentos compartilhados pelos serviços do controle financeiro. */
@Component
public class LedgerSupport {

    private final AccountRepository accounts;
    private final CurrencyService currencies;
    private final FinanceCategoryService categories;
    private final InstallmentPlanRepository plans;
    private final RecurringEntryRepository entries;

    public LedgerSupport(AccountRepository accounts, CurrencyService currencies, FinanceCategoryService categories,
            InstallmentPlanRepository plans, RecurringEntryRepository entries) {
        this.accounts = accounts;
        this.currencies = currencies;
        this.categories = categories;
        this.plans = plans;
        this.entries = entries;
    }

    public void requireAccount(UUID userId, UUID accountId) {
        if (accountId != null && accounts.findByIdAndUserId(accountId, userId).isEmpty()) {
            throw new NotFoundException("Conta");
        }
    }

    public void requireCurrency(String currency) {
        if (!currencies.isSupported(currency)) {
            throw new BusinessRuleException("Moeda não suportada.", "currency");
        }
    }

    /** Converte lançamentos em respostas resolvendo categorias e origens em lote (sem N+1). */
    @Transactional(readOnly = true)
    public List<TransactionResponse> toResponses(UUID userId, Collection<FinanceTransaction> list) {
        if (list.isEmpty()) {
            return List.of();
        }
        Map<UUID, FinanceCategory> cats = categories.index(userId);
        Map<UUID, InstallmentPlan> planIndex = plans.findAllById(list.stream()
                        .map(FinanceTransaction::getInstallmentPlanId).filter(Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(InstallmentPlan::getId, Function.identity()));
        Map<UUID, RecurringEntry> entryIndex = entries.findAllById(list.stream()
                        .map(FinanceTransaction::getRecurringEntryId).filter(Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(RecurringEntry::getId, Function.identity()));
        return list.stream().map(t -> toResponse(t, cats, planIndex, entryIndex)).toList();
    }

    static TransactionResponse toResponse(FinanceTransaction t, Map<UUID, FinanceCategory> cats,
            Map<UUID, InstallmentPlan> planIndex, Map<UUID, RecurringEntry> entryIndex) {
        TransactionSource source = TransactionSource.MANUAL;
        String label = null;
        if (t.getInstallmentPlanId() != null) {
            source = TransactionSource.INSTALLMENT;
            InstallmentPlan plan = planIndex.get(t.getInstallmentPlanId());
            label = "Parcela " + t.getInstallmentNumber() + (plan != null ? "/" + plan.getInstallmentsCount() : "");
        } else if (t.getRecurringEntryId() != null) {
            source = TransactionSource.RECURRING;
            RecurringEntry entry = entryIndex.get(t.getRecurringEntryId());
            label = entry != null ? entry.getName() : "Recorrência";
        }
        return new TransactionResponse(t.getId(), t.getKind(), t.getOccurredOn(), MoneyResponse.of(t.money()),
                t.getDescription(), FinanceCategoryService.ref(cats.get(t.getCategoryId())), t.getAccountId(),
                t.getStatus(), source, label, t.getInstallmentPlanId(), t.getRecurringEntryId());
    }
}
