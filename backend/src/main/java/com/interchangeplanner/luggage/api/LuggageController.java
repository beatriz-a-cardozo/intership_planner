package com.interchangeplanner.luggage.api;

import com.interchangeplanner.luggage.api.LuggageDtos.BagRequest;
import com.interchangeplanner.luggage.api.LuggageDtos.BagResponse;
import com.interchangeplanner.luggage.api.LuggageDtos.ItemRequest;
import com.interchangeplanner.luggage.api.LuggageDtos.ItemResponse;
import com.interchangeplanner.luggage.api.LuggageDtos.LuggageSummary;
import com.interchangeplanner.luggage.api.LuggageDtos.PackedRequest;
import com.interchangeplanner.luggage.application.LuggageService;
import com.interchangeplanner.luggage.domain.LuggageItem;
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
@RequestMapping("/api/luggage")
@Tag(name = "Mala")
public class LuggageController {

    private final LuggageService service;

    public LuggageController(LuggageService service) {
        this.service = service;
    }

    @GetMapping("/summary")
    @Operation(summary = "Malas com peso e alertas, não alocados e checklist")
    public LuggageSummary summary(CurrentUser user) {
        return service.summary(user.id());
    }

    @GetMapping("/bags")
    @Operation(summary = "Lista malas com o peso calculado")
    public List<BagResponse> bags(CurrentUser user) {
        return service.bags(user.id());
    }

    @PostMapping("/bags")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria mala")
    public BagResponse createBag(CurrentUser user, @Valid @RequestBody BagRequest r) {
        return service.createBag(user.id(), r);
    }

    @PutMapping("/bags/{id}")
    @Operation(summary = "Edita mala")
    public BagResponse updateBag(CurrentUser user, @PathVariable UUID id, @Valid @RequestBody BagRequest r) {
        return service.updateBag(user.id(), id, r);
    }

    @DeleteMapping("/bags/{id}")
    @Operation(summary = "Exclui mala (os itens voltam para não alocados)")
    public ResponseEntity<Void> deleteBag(CurrentUser user, @PathVariable UUID id) {
        service.deleteBag(user.id(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/items")
    @Operation(summary = "Lista itens (bagId, unallocated=true, category, packed)")
    public List<ItemResponse> items(CurrentUser user, @RequestParam(required = false) UUID bagId,
            @RequestParam(defaultValue = "false") boolean unallocated,
            @RequestParam(required = false) LuggageItem.Category category,
            @RequestParam(required = false) Boolean packed) {
        return service.items(user.id(), new LuggageService.ItemFilter(bagId, unallocated, category, packed));
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria item")
    public ItemResponse createItem(CurrentUser user, @Valid @RequestBody ItemRequest r) {
        return service.createItem(user.id(), r);
    }

    @PutMapping("/items/{id}")
    @Operation(summary = "Edita item (inclui mudar de mala)")
    public ItemResponse updateItem(CurrentUser user, @PathVariable UUID id, @Valid @RequestBody ItemRequest r) {
        return service.updateItem(user.id(), id, r);
    }

    @PatchMapping("/items/{id}/packed")
    @Operation(summary = "Marca como embalado / não embalado")
    public ItemResponse packed(CurrentUser user, @PathVariable UUID id, @Valid @RequestBody PackedRequest r) {
        return service.setPacked(user.id(), id, r.packed());
    }

    @DeleteMapping("/items/{id}")
    @Operation(summary = "Exclui item")
    public ResponseEntity<Void> deleteItem(CurrentUser user, @PathVariable UUID id) {
        service.deleteItem(user.id(), id);
        return ResponseEntity.noContent().build();
    }
}
