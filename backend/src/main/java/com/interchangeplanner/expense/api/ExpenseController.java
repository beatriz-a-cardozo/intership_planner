package com.interchangeplanner.expense.api;

import com.interchangeplanner.expense.application.ExpenseService;
import com.interchangeplanner.expense.application.ExpenseSummaryService;
import com.interchangeplanner.expense.domain.ExpenseStatus;
import com.interchangeplanner.shared.error.NotFoundException;
import com.interchangeplanner.shared.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/expenses")
@Tag(name = "Gastos")
public class ExpenseController {

    private final ExpenseService service;
    private final ExpenseSummaryService summaries;

    public ExpenseController(ExpenseService service, ExpenseSummaryService summaries) {
        this.service = service;
        this.summaries = summaries;
    }

    @GetMapping
    @Operation(summary = "Lista gastos do objetivo (padrão: principal), com filtros")
    public List<ExpenseResponse> list(CurrentUser user,
            @RequestParam(required = false) UUID objectiveId,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID cycleId,
            @RequestParam(required = false) ExpenseStatus status,
            @RequestParam(required = false) Boolean required,
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String q) {
        return service.list(user.id(),
                new ExpenseService.Filter(objectiveId, categoryId, cycleId, status, required, currency, from, to, q));
    }

    @GetMapping("/summary")
    @Operation(summary = "Totais e agrupamento por ciclo, categoria, periodicidade ou status")
    public ExpenseSummaryResponse summary(CurrentUser user, @RequestParam(required = false) UUID objectiveId,
            @RequestParam(defaultValue = "CYCLE") ExpenseSummaryResponse.GroupBy groupBy) {
        ExpenseSummaryResponse summary = summaries.summary(user.id(), objectiveId, groupBy);
        if (summary == null) {
            throw new NotFoundException("Objetivo");
        }
        return summary;
    }

    @PostMapping("/preview-total")
    @Operation(summary = "Calcula ocorrências e total sem salvar (formulário em tempo real)")
    public PreviewTotalResponse preview(CurrentUser user, @Valid @RequestBody PreviewTotalRequest request) {
        return service.preview(user.id(), request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe do gasto com pagamentos")
    public ExpenseDetailResponse get(CurrentUser user, @PathVariable UUID id) {
        return service.get(user.id(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria um gasto")
    public ExpenseDetailResponse create(CurrentUser user, @Valid @RequestBody ExpenseRequest request) {
        return service.create(user.id(), request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um gasto")
    public ExpenseDetailResponse update(CurrentUser user, @PathVariable UUID id,
            @Valid @RequestBody ExpenseRequest request) {
        return service.update(user.id(), id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Exclui o gasto e seus pagamentos")
    public ResponseEntity<Void> delete(CurrentUser user, @PathVariable UUID id) {
        service.delete(user.id(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancela o gasto (sai dos totais; histórico preservado)")
    public ExpenseDetailResponse cancel(CurrentUser user, @PathVariable UUID id) {
        return service.cancel(user.id(), id);
    }

    @PostMapping("/{id}/restore")
    @Operation(summary = "Reativa um gasto cancelado")
    public ExpenseDetailResponse restore(CurrentUser user, @PathVariable UUID id) {
        return service.restore(user.id(), id);
    }

    @GetMapping("/{id}/payments")
    @Operation(summary = "Pagamentos do gasto")
    public List<PaymentResponse> payments(CurrentUser user, @PathVariable UUID id) {
        return service.payments(user.id(), id);
    }

    @PostMapping("/{id}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra um pagamento (parcial ou total, em qualquer moeda)")
    public ExpenseDetailResponse addPayment(CurrentUser user, @PathVariable UUID id,
            @Valid @RequestBody PaymentRequest request) {
        return service.addPayment(user.id(), id, request);
    }

    @DeleteMapping("/{id}/payments/{paymentId}")
    @Operation(summary = "Remove um pagamento")
    public ExpenseDetailResponse deletePayment(CurrentUser user, @PathVariable UUID id, @PathVariable UUID paymentId) {
        return service.deletePayment(user.id(), id, paymentId);
    }
}
