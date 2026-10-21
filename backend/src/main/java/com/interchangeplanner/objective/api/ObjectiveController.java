package com.interchangeplanner.objective.api;

import com.interchangeplanner.objective.application.AdjustmentService;
import com.interchangeplanner.objective.application.ObjectiveProgressService;
import com.interchangeplanner.objective.application.ObjectiveService;
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
@RequestMapping("/api/objectives")
@Tag(name = "Objetivos")
public class ObjectiveController {

    private final ObjectiveService service;
    private final ObjectiveProgressService progress;
    private final AdjustmentService adjustments;

    public ObjectiveController(ObjectiveService service, ObjectiveProgressService progress,
            AdjustmentService adjustments) {
        this.service = service;
        this.progress = progress;
        this.adjustments = adjustments;
    }

    @GetMapping
    @Operation(summary = "Lista objetivos (principal primeiro)")
    public List<ObjectiveResponse> list(CurrentUser user) {
        return service.list(user.id());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria um objetivo (o primeiro vira o principal)")
    public ObjectiveResponse create(CurrentUser user, @Valid @RequestBody ObjectiveRequest request) {
        return service.create(user.id(), request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe do objetivo")
    public ObjectiveResponse get(CurrentUser user, @PathVariable UUID id) {
        return service.get(user.id(), id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza o objetivo")
    public ObjectiveResponse update(CurrentUser user, @PathVariable UUID id, @Valid @RequestBody ObjectiveRequest request) {
        return service.update(user.id(), id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Exclui o objetivo, seus gastos e ajustes (contas são desvinculadas)")
    public ResponseEntity<Void> delete(CurrentUser user, @PathVariable UUID id) {
        service.delete(user.id(), id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/primary")
    @Operation(summary = "Define como objetivo principal")
    public ObjectiveResponse makePrimary(CurrentUser user, @PathVariable UUID id) {
        return service.makePrimary(user.id(), id);
    }

    @GetMapping("/{id}/progress")
    @Operation(summary = "Meta, mobilizado, faltam e economia mensal necessária")
    public ObjectiveProgressResponse progress(CurrentUser user, @PathVariable UUID id) {
        return progress.progress(user.id(), id);
    }

    @GetMapping("/{id}/adjustments")
    @Operation(summary = "Ajustes manuais da necessidade planejada (ex.: Sperrkonto)")
    public List<AdjustmentResponse> adjustments(CurrentUser user, @PathVariable UUID id) {
        return adjustments.list(user.id(), id);
    }

    @PostMapping("/{id}/adjustments")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria um ajuste manual (valor com sinal)")
    public AdjustmentResponse createAdjustment(CurrentUser user, @PathVariable UUID id,
            @Valid @RequestBody AdjustmentRequest request) {
        return adjustments.create(user.id(), id, request);
    }

    @PutMapping("/{id}/adjustments/{adjustmentId}")
    @Operation(summary = "Atualiza um ajuste manual")
    public AdjustmentResponse updateAdjustment(CurrentUser user, @PathVariable UUID id, @PathVariable UUID adjustmentId,
            @Valid @RequestBody AdjustmentRequest request) {
        return adjustments.update(user.id(), id, adjustmentId, request);
    }

    @DeleteMapping("/{id}/adjustments/{adjustmentId}")
    @Operation(summary = "Remove um ajuste manual")
    public ResponseEntity<Void> deleteAdjustment(CurrentUser user, @PathVariable UUID id,
            @PathVariable UUID adjustmentId) {
        adjustments.delete(user.id(), id, adjustmentId);
        return ResponseEntity.noContent().build();
    }
}
