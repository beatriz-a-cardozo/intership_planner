package com.interchangeplanner.luggage.application;

import com.interchangeplanner.luggage.api.LuggageDtos.BagRequest;
import com.interchangeplanner.luggage.api.LuggageDtos.BagResponse;
import com.interchangeplanner.luggage.api.LuggageDtos.ItemRequest;
import com.interchangeplanner.luggage.api.LuggageDtos.ItemResponse;
import com.interchangeplanner.luggage.api.LuggageDtos.LuggageSummary;
import com.interchangeplanner.luggage.domain.LuggageBag;
import com.interchangeplanner.luggage.domain.LuggageBagRepository;
import com.interchangeplanner.luggage.domain.LuggageItem;
import com.interchangeplanner.luggage.domain.LuggageItemRepository;
import com.interchangeplanner.shared.error.BusinessRuleException;
import com.interchangeplanner.shared.error.NotFoundException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Malas e itens. Pesos e alertas vêm do {@link LuggageWeightCalculator}; excluir uma mala devolve os itens
 * para "não alocados" (FK com ON DELETE SET NULL).
 */
@Service
public class LuggageService {

    /** Filtro da listagem de itens; {@code unallocated} = só os sem mala. */
    public record ItemFilter(UUID bagId, boolean unallocated, LuggageItem.Category category, Boolean packed) {
    }

    private final LuggageBagRepository bags;
    private final LuggageItemRepository items;

    public LuggageService(LuggageBagRepository bags, LuggageItemRepository items) {
        this.bags = bags;
        this.items = items;
    }

    // ------------------------------------------------------------------ malas

    @Transactional(readOnly = true)
    public List<BagResponse> bags(UUID userId) {
        Map<UUID, List<LuggageItem>> byBag = itemsByBag(userId);
        return bags.findByUserIdOrderBySortOrderAscCreatedAtAsc(userId).stream()
                .map(b -> toResponse(b, byBag.getOrDefault(b.getId(), List.of())))
                .toList();
    }

    @Transactional
    public BagResponse createBag(UUID userId, BagRequest r) {
        int order = bags.findByUserIdOrderBySortOrderAscCreatedAtAsc(userId).size();
        LuggageBag bag = bags.save(new LuggageBag(userId, r.name(), r.type(), r.maxWeightG(), order));
        return toResponse(bag, List.of());
    }

    @Transactional
    public BagResponse updateBag(UUID userId, UUID id, BagRequest r) {
        LuggageBag bag = requireBag(userId, id);
        bag.update(r.name(), r.type(), r.maxWeightG());
        return toResponse(bag, itemsByBag(userId).getOrDefault(id, List.of()));
    }

    @Transactional
    public void deleteBag(UUID userId, UUID id) {
        bags.delete(requireBag(userId, id));
    }

    // ------------------------------------------------------------------ itens

    @Transactional(readOnly = true)
    public List<ItemResponse> items(UUID userId, ItemFilter f) {
        return items.findByUserIdOrderByNameAsc(userId).stream()
                .filter(i -> !f.unallocated() || i.getBagId() == null)
                .filter(i -> f.bagId() == null || f.bagId().equals(i.getBagId()))
                .filter(i -> f.category() == null || i.getCategory() == f.category())
                .filter(i -> f.packed() == null || i.isPacked() == f.packed())
                .map(LuggageService::toResponse)
                .toList();
    }

    @Transactional
    public ItemResponse createItem(UUID userId, ItemRequest r) {
        return toResponse(items.save(new LuggageItem(userId, toData(userId, r))));
    }

    @Transactional
    public ItemResponse updateItem(UUID userId, UUID id, ItemRequest r) {
        LuggageItem item = requireItem(userId, id);
        item.update(toData(userId, r));
        return toResponse(item);
    }

    @Transactional
    public ItemResponse setPacked(UUID userId, UUID id, boolean packed) {
        LuggageItem item = requireItem(userId, id);
        item.setPacked(packed);
        return toResponse(item);
    }

    @Transactional
    public void deleteItem(UUID userId, UUID id) {
        items.delete(requireItem(userId, id));
    }

    @Transactional(readOnly = true)
    public LuggageSummary summary(UUID userId) {
        List<LuggageItem> all = items.findByUserIdOrderByNameAsc(userId);
        List<BagResponse> bagList = bags(userId);
        List<LuggageItem> unallocated = all.stream().filter(i -> i.getBagId() == null).toList();
        int unallocatedWeight = unallocated.stream().map(LuggageItem::totalWeightG).filter(Objects::nonNull)
                .mapToInt(Integer::intValue).sum();
        int total = bagList.stream().mapToInt(BagResponse::weightG).sum() + unallocatedWeight;
        return new LuggageSummary(bagList, unallocated.size(), unallocatedWeight, all.size(),
                (int) all.stream().filter(LuggageItem::isPacked).count(),
                (int) all.stream().filter(i -> i.isRequired() && !i.isPacked()).count(), total);
    }

    // ------------------------------------------------------------------ apoio

    private LuggageItem.Data toData(UUID userId, ItemRequest r) {
        if (r.bagId() != null && bags.findByIdAndUserId(r.bagId(), userId).isEmpty()) {
            throw new BusinessRuleException("Mala não encontrada.", "bagId");
        }
        return new LuggageItem.Data(r.bagId(), r.name(), r.category(), r.quantity() == null ? 1 : r.quantity(),
                r.unitWeightG(), r.priority(), Boolean.TRUE.equals(r.required()), Boolean.TRUE.equals(r.packed()),
                r.notes());
    }

    private Map<UUID, List<LuggageItem>> itemsByBag(UUID userId) {
        return items.findByUserIdOrderByNameAsc(userId).stream()
                .filter(i -> i.getBagId() != null)
                .collect(Collectors.groupingBy(LuggageItem::getBagId));
    }

    private LuggageBag requireBag(UUID userId, UUID id) {
        return bags.findByIdAndUserId(id, userId).orElseThrow(() -> new NotFoundException("Mala"));
    }

    private LuggageItem requireItem(UUID userId, UUID id) {
        return items.findByIdAndUserId(id, userId).orElseThrow(() -> new NotFoundException("Item"));
    }

    static BagResponse toResponse(LuggageBag b, List<LuggageItem> list) {
        LuggageWeightCalculator.Result r = LuggageWeightCalculator.calculate(b.getMaxWeightG(),
                list.stream().map(i -> new LuggageWeightCalculator.Line(i.getQuantity(), i.getUnitWeightG())).toList());
        return new BagResponse(b.getId(), b.getName(), b.getType(), b.getMaxWeightG(), list.size(),
                (int) list.stream().filter(LuggageItem::isPacked).count(), r.weightG(), r.itemsWithoutWeight(),
                r.percent(), r.state(), r.excessG(), r.remainingG());
    }

    static ItemResponse toResponse(LuggageItem i) {
        return new ItemResponse(i.getId(), i.getBagId(), i.getName(), i.getCategory(), i.getQuantity(), i.getUnitWeightG(),
                i.totalWeightG(), i.getPriority(), i.isRequired(), i.isPacked(), i.getNotes());
    }
}
