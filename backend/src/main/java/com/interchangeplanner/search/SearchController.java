package com.interchangeplanner.search;

import com.interchangeplanner.search.SearchDtos.SearchResponse;
import com.interchangeplanner.search.SearchDtos.SearchType;
import com.interchangeplanner.shared.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Set;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
@Tag(name = "Busca")
public class SearchController {

    private final SearchService service;

    public SearchController(SearchService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Busca em objetivos, gastos, universidades, programas, tarefas, eventos, mala e parcelamentos "
            + "(sem acento; mínimo 2 letras; até 6 por tipo)")
    public SearchResponse search(CurrentUser user, @RequestParam(defaultValue = "") String q,
            @RequestParam(required = false) Set<SearchType> types) {
        return service.search(user.id(), q, types);
    }
}
