package com.interchangeplanner.finance.account.api;

import com.interchangeplanner.finance.account.application.AccountService;
import com.interchangeplanner.shared.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts")
@Tag(name = "Contas e saldos")
public class AccountController {

    private final AccountService service;

    public AccountController(AccountService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista as contas com o saldo mais recente")
    public List<AccountResponse> list(CurrentUser user) {
        return service.list(user.id());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria uma conta")
    public AccountResponse create(CurrentUser user, @Valid @RequestBody AccountRequest request) {
        return service.create(user.id(), request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza uma conta (a moeda não muda)")
    public AccountResponse update(CurrentUser user, @PathVariable UUID id, @Valid @RequestBody AccountRequest request) {
        return service.update(user.id(), id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Exclui a conta e seu histórico de saldos")
    public ResponseEntity<Void> delete(CurrentUser user, @PathVariable UUID id) {
        service.delete(user.id(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/balances")
    @Operation(summary = "Lança o saldo numa data (substitui se já houver saldo nessa data)")
    public BalanceSnapshotResponse recordBalance(CurrentUser user, @PathVariable UUID id,
            @Valid @RequestBody BalanceRequest request) {
        return service.recordBalance(user.id(), id, request);
    }

    @GetMapping("/{id}/balances")
    @Operation(summary = "Histórico de saldos da conta (mais recente primeiro)")
    public List<BalanceSnapshotResponse> history(CurrentUser user, @PathVariable UUID id) {
        return service.history(user.id(), id);
    }

    @PostMapping("/savings/{objectiveId}")
    @Operation(summary = "Guarda (ou retira, com valor negativo) dinheiro para o objetivo numa conta vinculada")
    public AccountResponse addSavings(CurrentUser user, @PathVariable UUID objectiveId,
            @Valid @RequestBody SavingsRequest request) {
        return service.addSavings(user.id(), objectiveId, request);
    }

    @GetMapping("/summary")
    @Operation(summary = "Saldos por moeda e total convertido para a moeda principal")
    public AccountsSummaryResponse summary(CurrentUser user) {
        return service.summary(user.id());
    }
}
