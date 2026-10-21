package com.interchangeplanner.expense.domain;

import com.interchangeplanner.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.util.UUID;

/** Item de catálogo do usuário (categoria ou ciclo de gasto): nome, cor e ícone editáveis. */
@MappedSuperclass
public abstract class CatalogItem extends BaseEntity {

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "name", nullable = false, length = 60)
    private String name;

    @Column(name = "color", nullable = false, length = 20)
    private String color;

    @Column(name = "icon", nullable = false, length = 40)
    private String icon;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "archived", nullable = false)
    private boolean archived;

    protected CatalogItem() {
    }

    protected CatalogItem(UUID userId, String name, String color, String icon, int sortOrder) {
        this.userId = userId;
        update(name, color, icon, sortOrder, false);
    }

    public void update(String name, String color, String icon, int sortOrder, boolean archived) {
        this.name = name.strip();
        this.color = color;
        this.icon = icon;
        this.sortOrder = sortOrder;
        this.archived = archived;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getColor() {
        return color;
    }

    public String getIcon() {
        return icon;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public boolean isArchived() {
        return archived;
    }
}
