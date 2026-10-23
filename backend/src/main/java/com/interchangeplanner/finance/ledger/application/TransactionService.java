package com.interchangeplanner.finance.ledger.application;

import com.interchangeplanner.finance.ledger.api.LedgerDtos.TransactionRequest;
import com.interchangeplanner.finance.ledger.api.LedgerDtos.TransactionResponse;
import com.interchangeplanner.finance.ledger.domain.EntryKind;
import com.interchangeplanner.finance.ledger.domain.FinanceTransaction;
import com.interchangeplanner.finance.ledger.domain.FinanceTransactionRepository;
import com.interchangeplanner.finance.ledger.domain.TransactionStatus;
import com.interchangeplanner.shared.error.BusinessRuleException;
import com.interchangeplanner.shared.error.ConflictException;
import com.interchangeplanner.shared.error.NotFoundException;
import com.interchangeplanner.shared.money.Money;
import com.interchangeplanner.user.application.UserClock;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionService {

    /** Janela máxima de uma listagem (evita varreduras enormes por engano). */
    static final long MAX_RANGE_DAYS = 3 * 366;

    public record Filter(LocalDate from, LocalDate to, EntryKind kind, UUID categoryId, TransactionStatus status) {
    }

    private final FinanceTransactionRepository transactions;
    private final FinanceCategoryService categories;
    private final LedgerSupport support;
    private final Clock clock;
    private final UserClock userClock;

    public TransactionService(FinanceTransactionRepository transactions, FinanceCategoryService categories,
            LedgerSupport support, UserClock userClock) {
        this.transactions = transactions;
        this.categories = categories;
        this.support = support;
        this.clock = userClock.clock();
        this.userClock = userClock;
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> list(UUID userId, Filter f) {
        if (f.from().isAfter(f.to())) {
            throw new BusinessRuleException("A data inicial deve ser anterior à final.", "from");
        }
        if (ChronoUnit.DAYS.between(f.from(), f.to()) > MAX_RANGE_DAYS) {
            throw new BusinessRuleException("Período máximo de 3 anos.", "to");
        }
        List<FinanceTransaction> list = transactions
                .findByUserIdAndOccurredOnBetweenOrderByOccurredOnAscCreatedAtAsc(userId, f.from(), f.to()).stream()
                .filter(t -> f.kind() == null || t.getKind() == f.kind())
                .filter(t -> f.categoryId() == null || f.categoryId().equals(t.getCategoryId()))
                .filter(t -> f.status() == null || t.getStatus() == f.status())
                .toList();
        return support.toResponses(userId, list);
    }

    @Transactional
    public TransactionResponse create(UUID userId, TransactionRequest r) {
        validate(userId, r);
        TransactionStatus status = r.status() != null ? r.status()
                : r.occurredOn().isAfter(userClock.today(userId)) ? TransactionStatus.PLANNED : TransactionStatus.CONFIRMED;
        FinanceTransaction t = transactions.save(FinanceTransaction.manual(userId, r.kind(), r.categoryId(),
                r.accountId(), r.occurredOn(), Money.of(r.amount(), r.currency()), r.description(), status,
                clock.instant()));
        return support.toResponses(userId, List.of(t)).getFirst();
    }

    /**
     * Edita valor, data, categoria e descrição. Lançamentos gerados continuam vinculados à origem
     * (uma edição na recorrência regenera só as ocorrências futuras não confirmadas).
     */
    @Transactional
    public TransactionResponse update(UUID userId, UUID id, TransactionRequest r) {
        FinanceTransaction t = require(userId, id);
        if (r.kind() != t.getKind()) {
            throw new BusinessRuleException("Não é possível trocar receita por despesa; exclua e crie outro lançamento.", "kind");
        }
        validate(userId, r);
        t.update(r.categoryId(), r.accountId(), r.occurredOn(), Money.of(r.amount(), r.currency()), r.description());
        if (r.status() != null) {
            t.setStatus(r.status(), clock.instant());
        }
        try {
            transactions.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Já existe uma ocorrência desta recorrência nessa data.", "occurredOn");
        }
        return support.toResponses(userId, List.of(t)).getFirst();
    }

    @Transactional
    public TransactionResponse setStatus(UUID userId, UUID id, TransactionStatus status) {
        FinanceTransaction t = require(userId, id);
        t.setStatus(status, clock.instant());
        return support.toResponses(userId, List.of(t)).getFirst();
    }

    /** Parcelas são geridas pelo parcelamento; ocorrências de recorrência podem ser excluídas (pular um mês). */
    @Transactional
    public void delete(UUID userId, UUID id) {
        FinanceTransaction t = require(userId, id);
        if (t.getInstallmentPlanId() != null) {
            throw new BusinessRuleException("Esta é uma parcela: edite ou exclua o parcelamento.", "id");
        }
        transactions.delete(t);
    }

    private void validate(UUID userId, TransactionRequest r) {
        support.requireCurrency(r.currency());
        categories.requireUsable(userId, r.categoryId(), r.kind());
        support.requireAccount(userId, r.accountId());
    }

    private FinanceTransaction require(UUID userId, UUID id) {
        return transactions.findByIdAndUserId(id, userId).orElseThrow(() -> new NotFoundException("Lançamento"));
    }
}
