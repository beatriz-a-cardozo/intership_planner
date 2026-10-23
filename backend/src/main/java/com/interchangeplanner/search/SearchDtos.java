package com.interchangeplanner.search;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

public final class SearchDtos {

    private SearchDtos() {
    }

    /** Tipos pesquisáveis, na ordem em que aparecem nos resultados. */
    public enum SearchType {
        OBJECTIVE,
        EXPENSE,
        UNIVERSITY,
        PROGRAM,
        TASK,
        EVENT,
        LUGGAGE,
        INSTALLMENT
    }

    /** @param url rota do frontend que abre o resultado */
    public record SearchResult(SearchType type, UUID id, String title,
            @Schema(types = {"string", "null"}) String subtitle, String url) {
    }

    public record SearchResponse(String query, List<SearchResult> results) {
    }
}
