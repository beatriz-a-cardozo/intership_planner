package com.interchangeplanner.luggage.domain;

import com.interchangeplanner.shared.domain.BaseEntity;
import com.interchangeplanner.shared.domain.Priority;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;

/** Item a levar. Sem mala = ainda não alocado. Peso unitário em gramas (opcional). */
@Entity
@Table(name = "luggage_item")
public class LuggageItem extends BaseEntity {

    public enum Category {
        DOCUMENTS,
        CLOTHING,
        ELECTRONICS,
        HYGIENE,
        HEALTH,
        ACCESSORIES,
        FOOD,
        OTHER
    }

    public record Data(UUID bagId, String name, Category category, int quantity, Integer unitWeightG,
            Priority priority, boolean required, boolean packed, String notes) {
    }

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "bag_id")
    private UUID bagId;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 16)
    private Category category;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "unit_weight_g")
    private Integer unitWeightG;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 10)
    private Priority priority;

    @Column(name = "required", nullable = false)
    private boolean required;

    @Column(name = "packed", nullable = false)
    private boolean packed;

    @Column(name = "notes", length = 500)
    private String notes;

    protected LuggageItem() {
    }

    public LuggageItem(UUID userId, Data data) {
        this.userId = userId;
        update(data);
    }

    public void update(Data d) {
        this.bagId = d.bagId();
        this.name = d.name().strip();
        this.category = d.category() == null ? Category.OTHER : d.category();
        this.quantity = d.quantity();
        this.unitWeightG = d.unitWeightG();
        this.priority = d.priority() == null ? Priority.MEDIUM : d.priority();
        this.required = d.required();
        this.packed = d.packed();
        this.notes = d.notes() == null || d.notes().isBlank() ? null : d.notes().strip();
    }

    public void setPacked(boolean packed) {
        this.packed = packed;
    }

    public void moveTo(UUID bagId) {
        this.bagId = bagId;
    }

    /** Peso total da linha; nulo sem peso unitário. */
    public Integer totalWeightG() {
        return unitWeightG == null ? null : quantity * unitWeightG;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getBagId() {
        return bagId;
    }

    public String getName() {
        return name;
    }

    public Category getCategory() {
        return category;
    }

    public int getQuantity() {
        return quantity;
    }

    public Integer getUnitWeightG() {
        return unitWeightG;
    }

    public Priority getPriority() {
        return priority;
    }

    public boolean isRequired() {
        return required;
    }

    public boolean isPacked() {
        return packed;
    }

    public String getNotes() {
        return notes;
    }
}
