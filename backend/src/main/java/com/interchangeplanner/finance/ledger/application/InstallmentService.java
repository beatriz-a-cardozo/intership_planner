package com.interchangeplanner.finance.ledger.application;

import com.interchangeplanner.finance.ledger.api.LedgerDtos.InstallmentItem;
import com.interchangeplanner.finance.ledger.api.LedgerDtos.InstallmentPlanRequest;
import com.interchangeplanner.finance.ledger.api.LedgerDtos.InstallmentPlanResponse;
import com.interchangeplanner.finance.ledger.domain.EntryKind;
import com.interchangeplanner.finance.ledger.domain.FinanceCategory;
import com.interchangeplanner.finance.ledger.domain.FinanceTransaction;
import com.interchangeplanner.finance.ledger.domain.FinanceTransactionRepository;
import com.interchangeplanner.finance.ledger.domain.InstallmentPlan;
import com.interchangeplanner.finance.ledger.domain.InstallmentPlanRepository;
import com.interchangeplanner.finance.ledger.domain.PaymentMethod;
import com.interchangeplanner.shared.error.BusinessRuleException;
import com.interchangeplanner.shared.error.NotFoundException;
import com.interchangeplanner.shared.money.Money;
import com.interchangeplanner.shared.money.MoneyResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Compras parceladas. Criar gera as parcelas como lançamentos PLANNED. Editar mantém as parcelas já
 * confirmadas e redistribui o saldo restante (total − pagas antes do app − confirmadas) nas parcelas em aberto,
 * com o ajuste de centavos na última. Excluir apaga as parcelas em aberto e mantém as confirmadas como histórico.
 * Um parcelamento já em andamento ("parcela 4 de 10") informa as parcelas pagas antes: elas não viram
 * lançamentos (o mês em que foram pagas não é reescrito), só contam como pagas.
 */
@Service
public class InstallmentService {

    private final InstallmentPlanRepository plans;
    private final FinanceTransactionRepository transactions;
    private final FinanceCategoryService categories;
    private final LedgerSupport support;

    public InstallmentService(InstallmentPlanRepository plans, FinanceTransactionRepository transactions,
            FinanceCategoryService categories, LedgerSupport support) {
        this.plans = plans;
        this.transactions = transactions;
        this.categories = categories;
        this.support = support;
    }

    @Transactional(readOnly = true)
    public List<InstallmentPlanResponse> list(UUID userId) {
        Map<UUID, FinanceCategory> cats = categories.index(userId);
        return plans.findByUserIdOrderByFirstDueDateDesc(userId).stream()
                .map(p -> toResponse(p, cats))
                .toList();
    }

    @Transactional(readOnly = true)
    public InstallmentPlanResponse get(UUID userId, UUID id) {
        return toResponse(require(userId, id), categories.index(userId));
    }

    @Transactional
    public InstallmentPlanResponse create(UUID userId, InstallmentPlanRequest r) {
        InstallmentPlan plan = plans.save(new InstallmentPlan(userId, toData(userId, r)));
        regenerate(plan, List.of());
        return toResponse(plan, categories.index(userId));
    }

    @Transactional
    public InstallmentPlanResponse update(UUID userId, UUID id, InstallmentPlanRequest r) {
        InstallmentPlan plan = require(userId, id);
        InstallmentPlan.Data data = toData(userId, r);
        List<FinanceTransaction> confirmed = transactions.findByInstallmentPlanIdOrderByInstallmentNumberAsc(id).stream()
                .filter(FinanceTransaction::isConfirmed)
                .toList();
        if (!confirmed.isEmpty()) {
            if (!confirmed.getFirst().getCurrency().equals(data.total().currency())) {
                throw new BusinessRuleException("Há parcelas confirmadas: a moeda não pode mudar.", "currency");
            }
            int lastConfirmed = confirmed.stream().mapToInt(FinanceTransaction::getInstallmentNumber).max().orElse(0);
            if (data.installmentsCount() < lastConfirmed) {
                throw new BusinessRuleException("Já há " + lastConfirmed + " parcela(s) confirmada(s).", "installmentsCount");
            }
            int firstConfirmed = confirmed.stream().mapToInt(FinanceTransaction::getInstallmentNumber).min().orElse(0);
            if (data.paidBeforeCount() >= firstConfirmed) {
                throw new BusinessRuleException("A parcela " + firstConfirmed + " já foi confirmada no app.",
                        "paidBeforeCount");
            }
            Money paid = confirmed.stream().map(FinanceTransaction::money).reduce(Money.zero(data.total().currency()), Money::plus);
            if (paid.compareTo(data.total()) > 0) {
                throw new BusinessRuleException("O total é menor do que o já pago nas parcelas confirmadas.", "totalAmount");
            }
        }
        plan.update(data);
        transactions.deletePlannedFromInstallment(id);
        regenerate(plan, confirmed);
        return toResponse(plans.findById(id).orElseThrow(), categories.index(userId));
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        InstallmentPlan plan = require(userId, id);
        transactions.deletePlannedFromInstallment(id);
        // as confirmadas ficam como histórico (FK com ON DELETE SET NULL)
        plans.delete(plans.findById(plan.getId()).orElseThrow());
    }

    /** Cria as parcelas em aberto, distribuindo o saldo restante entre os números ainda não confirmados. */
    private void regenerate(InstallmentPlan plan, List<FinanceTransaction> confirmed) {
        Money total = plan.total();
        InstallmentCalculator.Schedule schedule = schedule(plan);
        List<Integer> confirmedNumbers = confirmed.stream().map(FinanceTransaction::getInstallmentNumber).toList();
        List<InstallmentCalculator.Installment> open = schedule.open().stream()
                .filter(i -> !confirmedNumbers.contains(i.number()))
                .toList();
        if (open.isEmpty()) {
            return;
        }
        Money paid = confirmed.stream().map(FinanceTransaction::money).reduce(schedule.paidBefore(), Money::plus);
        Money remaining = total.minus(paid);
        List<InstallmentCalculator.Installment> amounts = remaining.isPositive()
                ? InstallmentCalculator.schedule(remaining, open.size(), open.getFirst().dueDate())
                : List.of();
        List<FinanceTransaction> created = new ArrayList<>();
        for (int i = 0; i < open.size(); i++) {
            InstallmentCalculator.Installment slot = open.get(i);
            Money value = amounts.isEmpty() ? Money.zero(total.currency()) : amounts.get(i).amount();
            if (!value.isPositive()) {
                continue;
            }
            created.add(FinanceTransaction.installment(plan.getUserId(), plan.getId(), slot.number(),
                    plan.getCategoryId(), plan.getAccountId(), slot.dueDate(), value, plan.getDescription()));
        }
        transactions.saveAll(created);
    }

    private InstallmentPlan.Data toData(UUID userId, InstallmentPlanRequest r) {
        support.requireCurrency(r.currency());
        categories.requireUsable(userId, r.categoryId(), EntryKind.EXPENSE);
        support.requireAccount(userId, r.accountId());
        if ((r.totalAmount() == null) == (r.installmentAmount() == null)) {
            throw new BusinessRuleException("Informe o valor total ou o valor da parcela (um dos dois).", "totalAmount");
        }
        Money total = r.totalAmount() != null
                ? Money.of(r.totalAmount(), r.currency())
                : InstallmentCalculator.totalFromInstallment(Money.of(r.installmentAmount(), r.currency()),
                        r.installmentsCount());
        int paidBefore = r.paidBeforeCount() == null ? 0 : r.paidBeforeCount();
        if (paidBefore >= r.installmentsCount()) {
            throw new BusinessRuleException("Deve faltar pelo menos uma parcela.", "paidBeforeCount");
        }
        return new InstallmentPlan.Data(r.categoryId(), r.accountId(), r.description(), total, r.installmentsCount(),
                paidBefore, r.firstDueDate(), r.paymentMethod() == null ? PaymentMethod.CREDIT_CARD : r.paymentMethod(),
                r.notes());
    }

    private static InstallmentCalculator.Schedule schedule(InstallmentPlan p) {
        return InstallmentCalculator.inProgress(p.total(), p.getInstallmentsCount(), p.getPaidBeforeCount(),
                p.getFirstDueDate());
    }

    private InstallmentPlan require(UUID userId, UUID id) {
        return plans.findByIdAndUserId(id, userId).orElseThrow(() -> new NotFoundException("Parcelamento"));
    }

    private InstallmentPlanResponse toResponse(InstallmentPlan p, Map<UUID, FinanceCategory> cats) {
        List<FinanceTransaction> items = transactions.findByInstallmentPlanIdOrderByInstallmentNumberAsc(p.getId());
        InstallmentCalculator.Schedule schedule = schedule(p);
        Money paid = schedule.paidBefore();
        int paidCount = p.getPaidBeforeCount();
        FinanceTransaction next = null;
        List<InstallmentItem> list = new ArrayList<>();
        for (FinanceTransaction t : items.stream().sorted(Comparator.comparing(FinanceTransaction::getInstallmentNumber)).toList()) {
            if (t.isConfirmed()) {
                paid = paid.plus(t.money());
                paidCount++;
            } else if (next == null) {
                next = t;
            }
            list.add(new InstallmentItem(t.getInstallmentNumber(), t.getOccurredOn(), MoneyResponse.of(t.money()),
                    t.getStatus(), t.getId()));
        }
        Money regular = schedule.open().getFirst().amount();
        int openCount = p.getInstallmentsCount() - p.getPaidBeforeCount();
        return new InstallmentPlanResponse(p.getId(), p.getDescription(), FinanceCategoryService.ref(cats.get(p.getCategoryId())),
                p.getAccountId(), MoneyResponse.of(p.total()), MoneyResponse.of(regular), p.getInstallmentsCount(),
                p.getPaidBeforeCount(), p.getFirstDueDate(), p.getFirstDueDate().plusMonths(openCount - 1L),
                p.getPaymentMethod(), p.getNotes(), paidCount, p.getInstallmentsCount() - paidCount,
                MoneyResponse.of(paid), MoneyResponse.of(p.total().minus(paid).atLeastZero()),
                next == null ? null : next.getOccurredOn(), list);
    }
}
