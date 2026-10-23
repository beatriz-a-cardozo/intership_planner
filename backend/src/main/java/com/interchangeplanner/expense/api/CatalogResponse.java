package com.interchangeplanner.expense.api;

import com.interchangeplanner.expense.domain.CatalogItem;
import com.interchangeplanner.expense.domain.ExpenseCycle;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

public record CatalogResponse(UUID id, String name, String color, String icon, int sortOrder, boolean archived,
        @Schema(types = {"boolean", "null"}, description = "Só ciclos: os gastos do ciclo entram na meta")
        Boolean inGoal) {

    public static CatalogResponse from(CatalogItem item) {
        return new CatalogResponse(item.getId(), item.getName(), item.getColor(), item.getIcon(), item.getSortOrder(),
                item.isArchived(), item instanceof ExpenseCycle c ? c.isInGoal() : null);
    }
}
