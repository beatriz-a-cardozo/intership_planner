package com.interchangeplanner.expense.api;

import com.interchangeplanner.expense.application.CatalogService;
import com.interchangeplanner.expense.application.ExpenseCategoryService;
import com.interchangeplanner.expense.application.ExpenseCycleService;
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

/** Endpoints de categorias e ciclos de gasto (mesmo contrato). */
public final class CatalogControllers {

    private CatalogControllers() {
    }

    abstract static class Base {

        private final CatalogService<?> service;

        Base(CatalogService<?> service) {
            this.service = service;
        }

        @GetMapping
        @Operation(summary = "Lista (inclui arquivados)")
        public List<CatalogResponse> list(CurrentUser user) {
            return service.list(user.id());
        }

        @PostMapping
        @ResponseStatus(HttpStatus.CREATED)
        @Operation(summary = "Cria")
        public CatalogResponse create(CurrentUser user, @Valid @RequestBody CatalogRequest request) {
            return service.create(user.id(), request);
        }

        @PutMapping("/{id}")
        @Operation(summary = "Atualiza (inclusive arquivar)")
        public CatalogResponse update(CurrentUser user, @PathVariable UUID id, @Valid @RequestBody CatalogRequest request) {
            return service.update(user.id(), id, request);
        }

        @DeleteMapping("/{id}")
        @Operation(summary = "Exclui (409 se estiver em uso)")
        public ResponseEntity<Void> delete(CurrentUser user, @PathVariable UUID id) {
            service.delete(user.id(), id);
            return ResponseEntity.noContent().build();
        }
    }

    @RestController
    @RequestMapping("/api/expense-categories")
    @Tag(name = "Categorias de gasto")
    public static class Categories extends Base {
        public Categories(ExpenseCategoryService service) {
            super(service);
        }
    }

    @RestController
    @RequestMapping("/api/expense-cycles")
    @Tag(name = "Ciclos de gasto")
    public static class Cycles extends Base {
        public Cycles(ExpenseCycleService service) {
            super(service);
        }
    }
}
