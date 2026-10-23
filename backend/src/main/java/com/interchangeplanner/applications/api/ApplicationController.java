package com.interchangeplanner.applications.api;

import com.interchangeplanner.applications.api.ApplicationDtos.ApplicationCreateRequest;
import com.interchangeplanner.applications.api.ApplicationDtos.ApplicationResponse;
import com.interchangeplanner.applications.api.ApplicationDtos.ApplicationUpdateRequest;
import com.interchangeplanner.applications.api.ApplicationDtos.RequestRequest;
import com.interchangeplanner.applications.api.ApplicationDtos.RequestResponse;
import com.interchangeplanner.applications.api.ApplicationDtos.StatusChangeRequest;
import com.interchangeplanner.applications.api.ApplicationDtos.StatusHistoryResponse;
import com.interchangeplanner.applications.application.ApplicationService;
import com.interchangeplanner.applications.domain.ApplicationStatus;
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
@RequestMapping("/api/applications")
@Tag(name = "Candidaturas")
public class ApplicationController {

    private final ApplicationService service;

    public ApplicationController(ApplicationService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista candidaturas (ativas por padrão)")
    public List<ApplicationResponse> list(CurrentUser user, @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(required = false) UUID objectiveId,
            @RequestParam(defaultValue = "false") boolean includeArchived) {
        return service.list(user.id(), status, objectiveId, includeArchived);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria candidatura (padrão: próximo ciclo do programa)")
    public ApplicationResponse create(CurrentUser user, @Valid @RequestBody ApplicationCreateRequest r) {
        return service.create(user.id(), r);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe da candidatura")
    public ApplicationResponse get(CurrentUser user, @PathVariable UUID id) {
        return service.get(user.id(), id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edita datas, estimativa de resposta e observações")
    public ApplicationResponse update(CurrentUser user, @PathVariable UUID id, @Valid @RequestBody ApplicationUpdateRequest r) {
        return service.update(user.id(), id, r);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Exclui a candidatura e seu histórico")
    public ResponseEntity<Void> delete(CurrentUser user, @PathVariable UUID id) {
        service.delete(user.id(), id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Muda o status (registra no histórico)")
    public ApplicationResponse changeStatus(CurrentUser user, @PathVariable UUID id, @Valid @RequestBody StatusChangeRequest r) {
        return service.changeStatus(user.id(), id, r);
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "Histórico de status")
    public List<StatusHistoryResponse> history(CurrentUser user, @PathVariable UUID id) {
        return service.history(user.id(), id);
    }

    @GetMapping("/{id}/requests")
    @Operation(summary = "Solicitações da universidade")
    public List<RequestResponse> requests(CurrentUser user, @PathVariable UUID id) {
        return service.requests(user.id(), id);
    }

    @PostMapping("/{id}/requests")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra uma solicitação (pendente => status \"Solicitado item\")")
    public RequestResponse createRequest(CurrentUser user, @PathVariable UUID id, @Valid @RequestBody RequestRequest r) {
        return service.createRequest(user.id(), id, r);
    }

    @PutMapping("/{id}/requests/{requestId}")
    @Operation(summary = "Atualiza (ex.: marca como atendida)")
    public RequestResponse updateRequest(CurrentUser user, @PathVariable UUID id, @PathVariable UUID requestId,
            @Valid @RequestBody RequestRequest r) {
        return service.updateRequest(user.id(), id, requestId, r);
    }

    @DeleteMapping("/{id}/requests/{requestId}")
    @Operation(summary = "Remove a solicitação")
    public ResponseEntity<Void> deleteRequest(CurrentUser user, @PathVariable UUID id, @PathVariable UUID requestId) {
        service.deleteRequest(user.id(), id, requestId);
        return ResponseEntity.noContent().build();
    }
}
