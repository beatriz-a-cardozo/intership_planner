package com.interchangeplanner.search;

import com.interchangeplanner.expense.domain.ExpenseRepository;
import com.interchangeplanner.finance.ledger.domain.InstallmentPlanRepository;
import com.interchangeplanner.luggage.domain.LuggageBag;
import com.interchangeplanner.luggage.domain.LuggageBagRepository;
import com.interchangeplanner.luggage.domain.LuggageItemRepository;
import com.interchangeplanner.objective.domain.ObjectiveRepository;
import com.interchangeplanner.search.SearchDtos.SearchResponse;
import com.interchangeplanner.search.SearchDtos.SearchResult;
import com.interchangeplanner.search.SearchDtos.SearchType;
import com.interchangeplanner.shared.text.TextKey;
import com.interchangeplanner.task.domain.TaskRepository;
import com.interchangeplanner.timeline.domain.TimelineEventRepository;
import com.interchangeplanner.university.domain.ProgramRepository;
import com.interchangeplanner.university.domain.University;
import com.interchangeplanner.university.domain.UniversityRepository;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Busca global. Os dados de uma pessoa são poucos (dezenas a centenas de registros por tipo), então a busca carrega
 * os nomes de cada tipo do usuário e compara sem acento e sem diferenciar maiúsculas ("munchen" acha "München").
 * Ordem: começa com o termo → contém no início de uma palavra → contém em qualquer lugar; no máximo 6 por tipo.
 */
@Service
public class SearchService {

    static final int MIN_LENGTH = 2;
    static final int PER_TYPE = 6;

    private final ObjectiveRepository objectives;
    private final ExpenseRepository expenses;
    private final UniversityRepository universities;
    private final ProgramRepository programs;
    private final TaskRepository tasks;
    private final TimelineEventRepository events;
    private final LuggageItemRepository items;
    private final LuggageBagRepository bags;
    private final InstallmentPlanRepository installments;

    public SearchService(ObjectiveRepository objectives, ExpenseRepository expenses, UniversityRepository universities,
            ProgramRepository programs, TaskRepository tasks, TimelineEventRepository events, LuggageItemRepository items,
            LuggageBagRepository bags, InstallmentPlanRepository installments) {
        this.objectives = objectives;
        this.expenses = expenses;
        this.universities = universities;
        this.programs = programs;
        this.tasks = tasks;
        this.events = events;
        this.items = items;
        this.bags = bags;
        this.installments = installments;
    }

    private record Candidate(SearchType type, UUID id, String title, String subtitle, String url) {
    }

    private record Scored(Candidate candidate, int score) {
    }

    @Transactional(readOnly = true)
    public SearchResponse search(UUID userId, String query, Set<SearchType> types) {
        String q = TextKey.of(query);
        if (q.length() < MIN_LENGTH) {
            return new SearchResponse(query == null ? "" : query.strip(), List.of());
        }
        Set<SearchType> wanted = types == null || types.isEmpty() ? EnumSet.allOf(SearchType.class) : types;
        List<SearchResult> results = new ArrayList<>();
        for (SearchType type : SearchType.values()) {
            if (!wanted.contains(type)) {
                continue;
            }
            candidates(userId, type)
                    .map(c -> new Scored(c, score(q, c.title(), c.subtitle())))
                    .filter(s -> s.score() > 0)
                    .sorted(Comparator.comparingInt(Scored::score).reversed()
                            .thenComparing(s -> s.candidate().title(), String.CASE_INSENSITIVE_ORDER))
                    .limit(PER_TYPE)
                    .map(Scored::candidate)
                    .forEach(c -> results.add(new SearchResult(c.type(), c.id(), c.title(), c.subtitle(), c.url())));
        }
        return new SearchResponse(query.strip(), results);
    }

    /** 3 = título começa com o termo; 2 = uma palavra do título começa com ele; 1 = contém (título ou subtítulo). */
    static int score(String q, String title, String subtitle) {
        String t = TextKey.of(title);
        if (t.startsWith(q)) {
            return 3;
        }
        if (t.contains(" " + q) || t.contains("-" + q) || t.contains("/" + q)) {
            return 2;
        }
        return t.contains(q) || TextKey.of(subtitle).contains(q) ? 1 : 0;
    }

    private Stream<Candidate> candidates(UUID userId, SearchType type) {
        return switch (type) {
            case OBJECTIVE -> objectives.findByUserIdOrderByPrimaryDescTargetDateAsc(userId).stream()
                    .map(o -> new Candidate(type, o.getId(), o.getName(), o.getCity(), "/objectives/" + o.getId()));
            case EXPENSE -> expenses.findByUserId(userId).stream()
                    .map(e -> new Candidate(type, e.getId(), e.getName(), e.getDescription(),
                            "/expenses?q=" + URLEncoder.encode(e.getName(), StandardCharsets.UTF_8).replace("+", "%20")));
            case UNIVERSITY -> universities.findByUserIdOrderByNameAsc(userId).stream()
                    .map(u -> new Candidate(type, u.getId(), u.getName(), u.getCity(), "/universities/" + u.getId()));
            case PROGRAM -> {
                Map<UUID, String> uniNames = universities.findByUserIdOrderByNameAsc(userId).stream()
                        .collect(Collectors.toMap(University::getId, University::getName));
                yield programs.findByUserIdOrderByNameAsc(userId).stream()
                        .map(p -> new Candidate(type, p.getId(), p.getName(), uniNames.get(p.getUniversityId()),
                                "/programs/" + p.getId()));
            }
            case TASK -> tasks.findByUserId(userId).stream()
                    .map(t -> new Candidate(type, t.getId(), t.getTitle(), t.getCategory(), "/tasks"));
            case EVENT -> events.findByUserId(userId).stream()
                    .map(e -> new Candidate(type, e.getId(), e.getTitle(), e.getDescription(), "/timeline"));
            case LUGGAGE -> {
                Map<UUID, String> bagNames = bags.findByUserIdOrderBySortOrderAscCreatedAtAsc(userId).stream()
                        .collect(Collectors.toMap(LuggageBag::getId, LuggageBag::getName));
                yield items.findByUserIdOrderByNameAsc(userId).stream()
                        .map(i -> new Candidate(type, i.getId(), i.getName(),
                                i.getBagId() == null ? "Ainda não alocado" : bagNames.get(i.getBagId()), "/luggage"));
            }
            case INSTALLMENT -> installments.findByUserIdOrderByFirstDueDateDesc(userId).stream()
                    .map(p -> new Candidate(type, p.getId(), p.getDescription(), p.getInstallmentsCount() + " parcelas",
                            "/finance?tab=installments"));
        };
    }
}
