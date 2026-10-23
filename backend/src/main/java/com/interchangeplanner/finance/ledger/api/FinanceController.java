package com.interchangeplanner.finance.ledger.api;

import com.interchangeplanner.finance.ledger.api.LedgerDtos.ActiveRequest;
import com.interchangeplanner.finance.ledger.api.LedgerDtos.CommitmentsResponse;
import com.interchangeplanner.finance.ledger.api.LedgerDtos.FinanceCategoryRequest;
import com.interchangeplanner.finance.ledger.api.LedgerDtos.FinanceCategoryResponse;
import com.interchangeplanner.finance.ledger.api.LedgerDtos.InstallmentPlanRequest;
import com.interchangeplanner.finance.ledger.api.LedgerDtos.InstallmentPlanResponse;
import com.interchangeplanner.finance.ledger.api.LedgerDtos.MonthlySummaryResponse;
import com.interchangeplanner.finance.ledger.api.LedgerDtos.OverviewResponse;
import com.interchangeplanner.finance.ledger.api.LedgerDtos.ProjectionResponse;
import com.interchangeplanner.finance.ledger.api.LedgerDtos.RecurringEntryRequest;
import com.interchangeplanner.finance.ledger.api.LedgerDtos.RecurringEntryResponse;
import com.interchangeplanner.finance.ledger.api.LedgerDtos.SavingGoalRequest;
import com.interchangeplanner.finance.ledger.api.LedgerDtos.SavingGoalResponse;
import com.interchangeplanner.finance.ledger.api.LedgerDtos.TransactionRequest;
import com.interchangeplanner.finance.ledger.api.LedgerDtos.TransactionResponse;
import com.interchangeplanner.finance.ledger.api.LedgerDtos.TransactionStatusRequest;
import com.interchangeplanner.finance.ledger.application.FinanceCategoryService;
import com.interchangeplanner.finance.ledger.application.FinanceSummaryService;
import com.interchangeplanner.finance.ledger.application.InstallmentService;
import com.interchangeplanner.finance.ledger.application.RecurringService;
import com.interchangeplanner.finance.ledger.application.SavingGoalService;
import com.interchangeplanner.finance.ledger.application.SavingsProjectionService;
import com.interchangeplanner.finance.ledger.application.TransactionService;
import com.interchangeplanner.finance.ledger.domain.EntryKind;
import com.interchangeplanner.finance.ledger.domain.TransactionStatus;
import com.interchangeplanner.shared.error.BusinessRuleException;
import com.interchangeplanner.shared.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "Controle financeiro")
public class FinanceController {

    private final FinanceCategoryService categories;
    private final TransactionService transactions;
    private final InstallmentService installments;
    private final RecurringService recurring;
    private final SavingGoalService goals;
    private final FinanceSummaryService summaries;
    private final SavingsProjectionService projection;
    private final Clock clock;

    public FinanceController(FinanceCategoryService categories, TransactionService transactions,
            InstallmentService installments, RecurringService recurring, SavingGoalService goals,
            FinanceSummaryService summaries, SavingsProjectionService projection, Clock clock) {
        this.categories = categories;
        this.transactions = transactions;
        this.installments = installments;
        this.recurring = recurring;
        this.goals = goals;
        this.summaries = summaries;
        this.projection = projection;
        this.clock = clock;
    }

    // ------------------------------------------------------------------ categorias

    @GetMapping("/finance/categories")
    @Operation(summary = "Categorias de receita e despesa")
    public List<FinanceCategoryResponse> categories(CurrentUser user) {
        return categories.list(user.id());
    }

    @PostMapping("/finance/categories")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria categoria")
    public FinanceCategoryResponse createCategory(CurrentUser user, @Valid @RequestBody FinanceCategoryRequest r) {
        return categories.create(user.id(), r);
    }

    @PutMapping("/finance/categories/{id}")
    @Operation(summary = "Atualiza categoria (inclusive arquivar)")
    public FinanceCategoryResponse updateCategory(CurrentUser user, @PathVariable UUID id,
            @Valid @RequestBody FinanceCategoryRequest r) {
        return categories.update(user.id(), id, r);
    }

    @DeleteMapping("/finance/categories/{id}")
    @Operation(summary = "Exclui categoria (409 se em uso)")
    public ResponseEntity<Void> deleteCategory(CurrentUser user, @PathVariable UUID id) {
        categories.delete(user.id(), id);
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------------ lançamentos

    @GetMapping("/finance/transactions")
    @Operation(summary = "Lançamentos de um período (padrão: mês atual)")
    public List<TransactionResponse> transactions(CurrentUser user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) EntryKind kind,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) TransactionStatus status) {
        YearMonth now = YearMonth.now(clock);
        return transactions.list(user.id(), new TransactionService.Filter(from != null ? from : now.atDay(1),
                to != null ? to : now.atEndOfMonth(), kind, categoryId, status));
    }

    @PostMapping("/finance/transactions")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria lançamento avulso")
    public TransactionResponse createTransaction(CurrentUser user, @Valid @RequestBody TransactionRequest r) {
        return transactions.create(user.id(), r);
    }

    @PutMapping("/finance/transactions/{id}")
    @Operation(summary = "Edita lançamento")
    public TransactionResponse updateTransaction(CurrentUser user, @PathVariable UUID id,
            @Valid @RequestBody TransactionRequest r) {
        return transactions.update(user.id(), id, r);
    }

    @PatchMapping("/finance/transactions/{id}/status")
    @Operation(summary = "Confirma ou volta para previsto")
    public TransactionResponse setStatus(CurrentUser user, @PathVariable UUID id,
            @Valid @RequestBody TransactionStatusRequest r) {
        return transactions.setStatus(user.id(), id, r.status());
    }

    @DeleteMapping("/finance/transactions/{id}")
    @Operation(summary = "Exclui lançamento (parcelas: via parcelamento)")
    public ResponseEntity<Void> deleteTransaction(CurrentUser user, @PathVariable UUID id) {
        transactions.delete(user.id(), id);
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------------ parcelamentos

    @GetMapping("/finance/installment-plans")
    @Operation(summary = "Parcelamentos com parcelas e situação")
    public List<InstallmentPlanResponse> installmentPlans(CurrentUser user) {
        return installments.list(user.id());
    }

    @GetMapping("/finance/installment-plans/{id}")
    @Operation(summary = "Detalhe do parcelamento")
    public InstallmentPlanResponse installmentPlan(CurrentUser user, @PathVariable UUID id) {
        return installments.get(user.id(), id);
    }

    @PostMapping("/finance/installment-plans")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria parcelamento e gera as parcelas previstas")
    public InstallmentPlanResponse createInstallmentPlan(CurrentUser user, @Valid @RequestBody InstallmentPlanRequest r) {
        return installments.create(user.id(), r);
    }

    @PutMapping("/finance/installment-plans/{id}")
    @Operation(summary = "Edita (regenera só as parcelas em aberto)")
    public InstallmentPlanResponse updateInstallmentPlan(CurrentUser user, @PathVariable UUID id,
            @Valid @RequestBody InstallmentPlanRequest r) {
        return installments.update(user.id(), id, r);
    }

    @DeleteMapping("/finance/installment-plans/{id}")
    @Operation(summary = "Exclui (parcelas confirmadas ficam como histórico)")
    public ResponseEntity<Void> deleteInstallmentPlan(CurrentUser user, @PathVariable UUID id) {
        installments.delete(user.id(), id);
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------------ recorrências

    @GetMapping("/finance/recurring")
    @Operation(summary = "Assinaturas e recorrências")
    public List<RecurringEntryResponse> recurring(CurrentUser user) {
        return recurring.list(user.id());
    }

    @PostMapping("/finance/recurring")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria recorrência e gera as próximas ocorrências")
    public RecurringEntryResponse createRecurring(CurrentUser user, @Valid @RequestBody RecurringEntryRequest r) {
        return recurring.create(user.id(), r);
    }

    @PutMapping("/finance/recurring/{id}")
    @Operation(summary = "Edita (regenera ocorrências futuras em aberto)")
    public RecurringEntryResponse updateRecurring(CurrentUser user, @PathVariable UUID id,
            @Valid @RequestBody RecurringEntryRequest r) {
        return recurring.update(user.id(), id, r);
    }

    @PatchMapping("/finance/recurring/{id}/active")
    @Operation(summary = "Pausa ou retoma")
    public RecurringEntryResponse setActive(CurrentUser user, @PathVariable UUID id, @Valid @RequestBody ActiveRequest r) {
        return recurring.setActive(user.id(), id, r.active());
    }

    @DeleteMapping("/finance/recurring/{id}")
    @Operation(summary = "Exclui (ocorrências confirmadas ficam como histórico)")
    public ResponseEntity<Void> deleteRecurring(CurrentUser user, @PathVariable UUID id) {
        recurring.delete(user.id(), id);
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------------ metas mensais

    @GetMapping("/finance/saving-goals")
    @Operation(summary = "Metas de economia por mês (padrão: ano corrente)")
    public List<SavingGoalResponse> savingGoals(CurrentUser user, @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        YearMonth now = YearMonth.now(clock);
        return goals.list(user.id(), parse(from, now.withMonth(1), "from"), parse(to, now.withMonth(12), "to"));
    }

    @PutMapping("/finance/saving-goals/{month}")
    @Operation(summary = "Define a meta de economia do mês (yyyy-MM)")
    public SavingGoalResponse setSavingGoal(CurrentUser user, @PathVariable String month,
            @Valid @RequestBody SavingGoalRequest r) {
        return goals.set(user.id(), parse(month, null, "month"), r);
    }

    @DeleteMapping("/finance/saving-goals/{month}")
    @Operation(summary = "Remove a meta do mês")
    public ResponseEntity<Void> deleteSavingGoal(CurrentUser user, @PathVariable String month) {
        goals.delete(user.id(), parse(month, null, "month"));
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------------ visões

    @GetMapping("/finance/summary")
    @Operation(summary = "Visão do mês: receitas, despesas, sobra, meta, por categoria, lançamentos")
    public MonthlySummaryResponse summary(CurrentUser user, @RequestParam(required = false) String month) {
        return summaries.monthly(user.id(), parse(month, YearMonth.now(clock), "month"));
    }

    @GetMapping("/finance/overview")
    @Operation(summary = "Série mensal para gráficos (padrão: 5 meses atrás até 6 à frente)")
    public OverviewResponse overview(CurrentUser user, @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        YearMonth now = YearMonth.now(clock);
        return summaries.overview(user.id(), parse(from, now.minusMonths(5), "from"), parse(to, now.plusMonths(6), "to"));
    }

    @GetMapping("/finance/commitments")
    @Operation(summary = "Parcelas e recorrências previstas nos próximos meses")
    public CommitmentsResponse commitments(CurrentUser user, @RequestParam(defaultValue = "12") int months) {
        return summaries.commitments(user.id(), months);
    }

    @GetMapping("/objectives/{id}/projection")
    @Operation(summary = "Previsão de quando a meta é atingida, com base no controle financeiro")
    public ProjectionResponse projection(CurrentUser user, @PathVariable UUID id) {
        return projection.project(user.id(), id);
    }

    private static YearMonth parse(String value, YearMonth fallback, String field) {
        if (value == null || value.isBlank()) {
            if (fallback == null) {
                throw new BusinessRuleException("Informe o mês (aaaa-mm).", field);
            }
            return fallback;
        }
        try {
            return YearMonth.parse(value);
        } catch (DateTimeParseException e) {
            throw new BusinessRuleException("Mês inválido; use aaaa-mm.", field);
        }
    }
}
