package com.interchangeplanner.finance.ledger.application;

import com.interchangeplanner.finance.ledger.api.LedgerDtos.CategoryRef;
import com.interchangeplanner.finance.ledger.api.LedgerDtos.FinanceCategoryRequest;
import com.interchangeplanner.finance.ledger.api.LedgerDtos.FinanceCategoryResponse;
import com.interchangeplanner.finance.ledger.domain.EntryKind;
import com.interchangeplanner.finance.ledger.domain.FinanceCategory;
import com.interchangeplanner.finance.ledger.domain.FinanceCategoryRepository;
import com.interchangeplanner.finance.ledger.domain.FinanceTransactionRepository;
import com.interchangeplanner.finance.ledger.domain.InstallmentPlanRepository;
import com.interchangeplanner.finance.ledger.domain.RecurringEntryRepository;
import com.interchangeplanner.shared.error.BusinessRuleException;
import com.interchangeplanner.shared.error.ConflictException;
import com.interchangeplanner.shared.error.NotFoundException;
import com.interchangeplanner.user.domain.UserRegisteredEvent;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Categorias de receita e despesa do controle financeiro. A migration V5 semeia a mesma lista para quem já existia. */
@Service
public class FinanceCategoryService {

    record Seed(EntryKind kind, String name, String color, String icon) {
    }

    static final List<Seed> DEFAULTS = List.of(
            new Seed(EntryKind.INCOME, "Salário", "#1f4d3f", "briefcase"),
            new Seed(EntryKind.INCOME, "Freelance", "#2f6d9a", "laptop"),
            new Seed(EntryKind.INCOME, "Outras receitas", "#0f766e", "plus-circle"),
            new Seed(EntryKind.EXPENSE, "Moradia", "#b45309", "home"),
            new Seed(EntryKind.EXPENSE, "Alimentação", "#ea580c", "utensils"),
            new Seed(EntryKind.EXPENSE, "Transporte", "#4d7c0f", "bike"),
            new Seed(EntryKind.EXPENSE, "Saúde", "#dc2626", "heart-pulse"),
            new Seed(EntryKind.EXPENSE, "Lazer", "#db2777", "sparkles"),
            new Seed(EntryKind.EXPENSE, "Assinaturas", "#7c3aed", "repeat"),
            new Seed(EntryKind.EXPENSE, "Educação", "#2563eb", "graduation-cap"),
            new Seed(EntryKind.EXPENSE, "Outros", "#6b7280", "tag"));

    private final FinanceCategoryRepository categories;
    private final FinanceTransactionRepository transactions;
    private final InstallmentPlanRepository installments;
    private final RecurringEntryRepository recurring;

    public FinanceCategoryService(FinanceCategoryRepository categories, FinanceTransactionRepository transactions,
            InstallmentPlanRepository installments, RecurringEntryRepository recurring) {
        this.categories = categories;
        this.transactions = transactions;
        this.installments = installments;
        this.recurring = recurring;
    }

    @EventListener
    public void onUserRegistered(UserRegisteredEvent event) {
        seedFor(event.userId());
    }

    @Transactional
    public void seedFor(UUID userId) {
        if (categories.existsByUserId(userId)) {
            return;
        }
        int order = 0;
        EntryKind previous = null;
        for (Seed s : DEFAULTS) {
            order = s.kind() == previous ? order + 1 : 1;
            previous = s.kind();
            categories.save(new FinanceCategory(userId, s.kind(), s.name(), s.color(), s.icon(), order));
        }
    }

    @Transactional(readOnly = true)
    public List<FinanceCategoryResponse> list(UUID userId) {
        return categories.findByUserIdOrderByKindAscSortOrderAscNameAsc(userId).stream().map(FinanceCategoryService::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Map<UUID, FinanceCategory> index(UUID userId) {
        return categories.findByUserIdOrderByKindAscSortOrderAscNameAsc(userId).stream()
                .collect(Collectors.toMap(FinanceCategory::getId, Function.identity()));
    }

    /** Categoria do usuário, do tipo esperado e não arquivada (para novos lançamentos). */
    @Transactional(readOnly = true)
    public FinanceCategory requireUsable(UUID userId, UUID id, EntryKind kind) {
        FinanceCategory category = require(userId, id);
        if (category.getKind() != kind) {
            throw new BusinessRuleException(kind == EntryKind.INCOME
                    ? "Escolha uma categoria de receita." : "Escolha uma categoria de despesa.", "categoryId");
        }
        if (category.isArchived()) {
            throw new BusinessRuleException("Esta categoria está arquivada.", "categoryId");
        }
        return category;
    }

    @Transactional
    public FinanceCategoryResponse create(UUID userId, FinanceCategoryRequest r) {
        if (categories.existsByUserIdAndKindAndNameIgnoreCase(userId, r.kind(), r.name().strip())) {
            throw new ConflictException("Já existe uma categoria com esse nome.", "name");
        }
        int order = r.sortOrder() != null ? r.sortOrder() : (int) categories.findByUserIdOrderByKindAscSortOrderAscNameAsc(userId)
                .stream().filter(c -> c.getKind() == r.kind()).count() + 1;
        return toResponse(categories.save(new FinanceCategory(userId, r.kind(), r.name(), r.color().toLowerCase(),
                r.icon(), order)));
    }

    @Transactional
    public FinanceCategoryResponse update(UUID userId, UUID id, FinanceCategoryRequest r) {
        FinanceCategory category = require(userId, id);
        if (r.kind() != category.getKind()) {
            throw new BusinessRuleException("O tipo da categoria não pode ser alterado.", "kind");
        }
        if (categories.existsByUserIdAndKindAndNameIgnoreCaseAndIdNot(userId, r.kind(), r.name().strip(), id)) {
            throw new ConflictException("Já existe uma categoria com esse nome.", "name");
        }
        category.update(r.name(), r.color().toLowerCase(), r.icon(),
                r.sortOrder() != null ? r.sortOrder() : category.getSortOrder(), Boolean.TRUE.equals(r.archived()));
        return toResponse(category);
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        FinanceCategory category = require(userId, id);
        if (transactions.existsByCategoryId(id) || installments.existsByCategoryId(id) || recurring.existsByCategoryId(id)) {
            throw new ConflictException("Categoria em uso. Arquive em vez de excluir.", "id");
        }
        categories.delete(category);
    }

    private FinanceCategory require(UUID userId, UUID id) {
        return categories.findByIdAndUserId(id, userId).orElseThrow(() -> new NotFoundException("Categoria"));
    }

    static FinanceCategoryResponse toResponse(FinanceCategory c) {
        return new FinanceCategoryResponse(c.getId(), c.getKind(), c.getName(), c.getColor(), c.getIcon(),
                c.getSortOrder(), c.isArchived());
    }

    public static CategoryRef ref(FinanceCategory c) {
        return c == null ? null : new CategoryRef(c.getId(), c.getName(), c.getColor(), c.getIcon());
    }
}
