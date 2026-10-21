package com.interchangeplanner.expense.api;

import com.interchangeplanner.expense.domain.CatalogItem;
import java.util.UUID;

public record CatalogResponse(UUID id, String name, String color, String icon, int sortOrder, boolean archived) {

    public static CatalogResponse from(CatalogItem item) {
        return new CatalogResponse(item.getId(), item.getName(), item.getColor(), item.getIcon(), item.getSortOrder(),
                item.isArchived());
    }
}
