package com.interchangeplanner.finance.ledger.domain;

import com.interchangeplanner.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;

/** Categoria de receita ou despesa do controle financeiro (separada das categorias de gasto do objetivo). */
@Entity
@Table(name = "finance_category")
public class FinanceCategory extends BaseEntity {

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 10, updatable = false)
    private EntryKind kind;

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

    protected FinanceCategory() {
    }

    public FinanceCategory(UUID userId, EntryKind kind, String name, String color, String icon, int sortOrder) {
        this.userId = userId;
        this.kind = kind;
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

    public EntryKind getKind() {
        return kind;
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
