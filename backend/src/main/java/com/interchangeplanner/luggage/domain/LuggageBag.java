package com.interchangeplanner.luggage.domain;

import com.interchangeplanner.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;

/** Mala com limite de peso opcional (em gramas). */
@Entity
@Table(name = "luggage_bag")
public class LuggageBag extends BaseEntity {

    public enum Type {
        CARRY_ON,
        CHECKED,
        PERSONAL_ITEM,
        OTHER
    }

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "name", nullable = false, length = 80)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 16)
    private Type type;

    @Column(name = "max_weight_g")
    private Integer maxWeightG;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected LuggageBag() {
    }

    public LuggageBag(UUID userId, String name, Type type, Integer maxWeightG, int sortOrder) {
        this.userId = userId;
        this.sortOrder = sortOrder;
        update(name, type, maxWeightG);
    }

    public void update(String name, Type type, Integer maxWeightG) {
        this.name = name.strip();
        this.type = type == null ? Type.CHECKED : type;
        this.maxWeightG = maxWeightG;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    public Integer getMaxWeightG() {
        return maxWeightG;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}
