package com.interchangeplanner.finance.ledger.application;

import com.interchangeplanner.finance.ledger.api.LedgerDtos.RecurringEntryRequest;
import com.interchangeplanner.finance.ledger.api.LedgerDtos.RecurringEntryResponse;
import com.interchangeplanner.finance.ledger.domain.FinanceCategory;
import com.interchangeplanner.finance.ledger.domain.FinanceTransaction;
import com.interchangeplanner.finance.ledger.domain.FinanceTransactionRepository;
import com.interchangeplanner.finance.ledger.domain.Frequency;
import com.interchangeplanner.finance.ledger.domain.RecurringEntry;
import com.interchangeplanner.finance.ledger.domain.RecurringEntryRepository;
import com.interchangeplanner.shared.error.BusinessRuleException;
import com.interchangeplanner.shared.error.NotFoundException;
import com.interchangeplanner.shared.money.Money;
import com.interchangeplanner.shared.money.MoneyResponse;
import com.interchangeplanner.user.application.UserClock;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Assinaturas, contas fixas e receitas recorrentes. As ocorrências são materializadas como lançamentos
 * PLANNED do início do mês corrente até {@code app.finance.materialize-months} meses à frente (padrão 36),
 * para a visão mensal, o comprometido futuro e a projeção lerem uma única tabela.
 *
 * <p>Editar ou pausar regenera só as ocorrências futuras ainda não confirmadas; as confirmadas são histórico.</p>
 */
@Service
public class RecurringService {

    private final RecurringEntryRepository entries;
    private final FinanceTransactionRepository transactions;
    private final FinanceCategoryService categories;
    private final LedgerSupport support;
    private final Clock clock;
    private final UserClock userClock;
    private final int horizonMonths;

    public RecurringService(RecurringEntryRepository entries, FinanceTransactionRepository transactions,
            FinanceCategoryService categories, LedgerSupport support, UserClock userClock,
            @Value("${app.finance.materialize-months:36}") int horizonMonths) {
        this.entries = entries;
        this.transactions = transactions;
        this.categories = categories;
        this.support = support;
        this.clock = userClock.clock();
        this.userClock = userClock;
        this.horizonMonths = horizonMonths;
    }

    @Transactional(readOnly = true)
    public List<RecurringEntryResponse> list(UUID userId) {
        Map<UUID, FinanceCategory> cats = categories.index(userId);
        return entries.findByUserIdOrderByActiveDescNameAsc(userId).stream().map(e -> toResponse(e, cats)).toList();
    }

    @Transactional
    public RecurringEntryResponse create(UUID userId, RecurringEntryRequest r) {
        RecurringEntry entry = entries.save(new RecurringEntry(userId, toData(userId, r)));
        sync(entry);
        return toResponse(entry, categories.index(userId));
    }

    @Transactional
    public RecurringEntryResponse update(UUID userId, UUID id, RecurringEntryRequest r) {
        RecurringEntry entry = require(userId, id);
        entry.update(toData(userId, r));
        sync(entry);
        return toResponse(entries.findById(id).orElseThrow(), categories.index(userId));
    }

    @Transactional
    public RecurringEntryResponse setActive(UUID userId, UUID id, boolean active) {
        RecurringEntry entry = require(userId, id);
        entry.setActive(active);
        sync(entry);
        return toResponse(entries.findById(id).orElseThrow(), categories.index(userId));
    }

    /** Exclui a recorrência e as ocorrências futuras em aberto; as confirmadas ficam como histórico. */
    @Transactional
    public void delete(UUID userId, UUID id) {
        require(userId, id);
        transactions.deletePlannedFromRecurring(id, LocalDate.of(1900, 1, 1));
        entries.delete(entries.findById(id).orElseThrow());
    }

    /**
     * Regenera as ocorrências em aberto a partir do início do mês corrente: apaga as PLANNED e recria as que
     * faltam (datas já confirmadas são preservadas).
     */
    @Transactional
    public void sync(RecurringEntry entry) {
        LocalDate from = windowStart();
        transactions.deletePlannedFromRecurring(entry.getId(), from);
        if (entry.isActive()) {
            materialize(entry, from);
        }
    }

    /** Job diário: estende a janela de todas as recorrências ativas e confirma as automáticas vencidas. */
    @Transactional
    public int extendAndAutoConfirm() {
        int created = 0;
        for (RecurringEntry entry : entries.findByActiveTrue()) {
            created += materialize(entry, windowStart());
        }
        transactions.autoConfirmDue(LocalDate.now(clock), clock.instant());
        return created;
    }

    private int materialize(RecurringEntry entry, LocalDate from) {
        LocalDate start = entry.getStartDate().isAfter(from) ? entry.getStartDate() : from;
        LocalDate to = LocalDate.now(clock).plusMonths(horizonMonths);
        List<LocalDate> dates = RecurrenceScheduler.occurrences(entry.rule(), start, to);
        if (dates.isEmpty()) {
            return 0;
        }
        Set<LocalDate> existing = new HashSet<>();
        transactions.findByRecurringEntryIdAndOccurredOnBetween(entry.getId(), dates.getFirst(), dates.getLast())
                .forEach(t -> existing.add(t.getOccurredOn()));
        List<FinanceTransaction> created = dates.stream()
                .filter(d -> !existing.contains(d))
                .map(d -> FinanceTransaction.recurring(entry.getUserId(), entry.getId(), entry.getKind(),
                        entry.getCategoryId(), entry.getAccountId(), d, entry.money(), entry.getName()))
                .toList();
        transactions.saveAll(created);
        return created.size();
    }

    private LocalDate windowStart() {
        return LocalDate.now(clock).withDayOfMonth(1);
    }

    private RecurringEntry.Data toData(UUID userId, RecurringEntryRequest r) {
        support.requireCurrency(r.currency());
        categories.requireUsable(userId, r.categoryId(), r.kind());
        support.requireAccount(userId, r.accountId());
        if (r.frequency() == Frequency.EVERY_N_MONTHS && r.intervalMonths() == null) {
            throw new BusinessRuleException("Informe a cada quantos meses.", "intervalMonths");
        }
        if (r.endDate() != null && r.endDate().isBefore(r.startDate())) {
            throw new BusinessRuleException("A data final deve ser depois do início.", "endDate");
        }
        return new RecurringEntry.Data(r.categoryId(), r.accountId(), r.name(), r.kind(),
                Money.of(r.amount(), r.currency()), r.frequency(), r.intervalMonths(), r.billingDay(), r.startDate(),
                r.endDate(), Boolean.TRUE.equals(r.autoConfirm()), r.notes());
    }

    private RecurringEntry require(UUID userId, UUID id) {
        return entries.findByIdAndUserId(id, userId).orElseThrow(() -> new NotFoundException("Recorrência"));
    }

    private RecurringEntryResponse toResponse(RecurringEntry e, Map<UUID, FinanceCategory> cats) {
        LocalDate today = userClock.today(e.getUserId());
        LocalDate next = e.isActive()
                ? RecurrenceScheduler.occurrences(e.rule(), today, today.plusYears(2)).stream().findFirst().orElse(null)
                : null;
        return new RecurringEntryResponse(e.getId(), e.getName(), e.getKind(), MoneyResponse.of(e.money()),
                FinanceCategoryService.ref(cats.get(e.getCategoryId())), e.getAccountId(), e.getFrequency(),
                e.getIntervalMonths(), e.getBillingDay(), e.getStartDate(), e.getEndDate(), e.isActive(),
                e.isAutoConfirm(), e.getNotes(), next, MoneyResponse.of(monthlyEquivalent(e)));
    }

    /** Valor por mês: semanal × 52/12; anual ÷ 12; a cada N meses ÷ N. */
    static Money monthlyEquivalent(RecurringEntry e) {
        Money amount = e.money();
        return switch (e.getFrequency()) {
            case WEEKLY -> Money.of(amount.amount().multiply(BigDecimal.valueOf(52))
                    .divide(BigDecimal.valueOf(12), Money.SCALE, Money.ROUNDING), amount.currency());
            case MONTHLY -> amount;
            case YEARLY -> amount.dividedBy(12);
            case EVERY_N_MONTHS -> amount.dividedBy(e.getIntervalMonths());
        };
    }
}
