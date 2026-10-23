package com.interchangeplanner.luggage.api;

import com.interchangeplanner.luggage.application.LuggageWeightCalculator.State;
import com.interchangeplanner.luggage.domain.LuggageBag;
import com.interchangeplanner.luggage.domain.LuggageItem;
import com.interchangeplanner.shared.domain.Priority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public final class LuggageDtos {

    private LuggageDtos() {
    }

    public record BagRequest(
            @NotBlank(message = "Informe o nome.") @Size(max = 80, message = "Máximo de 80 caracteres.") String name,
            LuggageBag.Type type,
            @Min(value = 1, message = "Limite maior que zero.") @Max(value = 100000, message = "Máximo de 100 kg.")
            Integer maxWeightG) {
    }

    /**
     * Mala com o peso calculado (gramas).
     *
     * @param itemsWithoutWeight itens sem peso informado (o total pode estar subestimado)
     * @param percent            peso/limite em % (nulo sem limite)
     * @param state              NO_LIMIT · OK (≤ 90%) · WARNING (> 90%) · OVER (> 100%)
     */
    public record BagResponse(
            UUID id,
            String name,
            LuggageBag.Type type,
            @Schema(types = {"integer", "null"}) Integer maxWeightG,
            int itemCount,
            int packedCount,
            int weightG,
            int itemsWithoutWeight,
            @Schema(types = {"number", "null"}) BigDecimal percent,
            State state,
            int excessG,
            @Schema(types = {"integer", "null"}) Integer remainingG) {
    }

    /** @param bagId nulo = ainda não alocado */
    public record ItemRequest(
            UUID bagId,
            @NotBlank(message = "Informe o item.") @Size(max = 120, message = "Máximo de 120 caracteres.") String name,
            LuggageItem.Category category,
            @Min(value = 1, message = "Mínimo 1.") @Max(value = 999, message = "Máximo 999.") Integer quantity,
            @Min(value = 0, message = "Peso não pode ser negativo.") @Max(value = 100000, message = "Máximo de 100 kg.")
            Integer unitWeightG,
            Priority priority,
            Boolean required,
            Boolean packed,
            @Size(max = 500, message = "Máximo de 500 caracteres.") String notes) {
    }

    public record PackedRequest(@NotNull Boolean packed) {
    }

    public record ItemResponse(
            UUID id,
            @Schema(types = {"string", "null"}, format = "uuid") UUID bagId,
            String name,
            LuggageItem.Category category,
            int quantity,
            @Schema(types = {"integer", "null"}) Integer unitWeightG,
            @Schema(types = {"integer", "null"}) Integer totalWeightG,
            Priority priority,
            boolean required,
            boolean packed,
            @Schema(types = {"string", "null"}) String notes) {
    }

    /**
     * Visão geral: malas com peso, itens não alocados e o checklist.
     *
     * @param requiredNotPacked itens obrigatórios ainda fora da mala
     */
    public record LuggageSummary(
            List<BagResponse> bags,
            int unallocatedCount,
            int unallocatedWeightG,
            int totalItems,
            int packedItems,
            int requiredNotPacked,
            int totalWeightG) {
    }
}
