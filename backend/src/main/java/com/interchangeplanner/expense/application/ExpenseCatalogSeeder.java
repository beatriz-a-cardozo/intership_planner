package com.interchangeplanner.expense.application;

import com.interchangeplanner.expense.domain.ExpenseCategory;
import com.interchangeplanner.expense.domain.ExpenseCategoryRepository;
import com.interchangeplanner.expense.domain.ExpenseCycle;
import com.interchangeplanner.expense.domain.ExpenseCycleRepository;
import com.interchangeplanner.user.domain.UserRegisteredEvent;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Catálogo padrão de cada usuário: os 4 ciclos e as categorias da planilha (com a grafia corrigida)
 * mais Viagem e Transporte. Tudo editável depois. A migration V4 aplica a MESMA lista aos usuários
 * que já existiam — manter as duas em sincronia.
 */
@Component
public class ExpenseCatalogSeeder {

    record Seed(String name, String color, String icon) {
    }

    static final List<Seed> CYCLES = List.of(
            new Seed("Preparação", "#1f4d3f", "clipboard-list"),
            new Seed("Imigração", "#2f6d9a", "plane"),
            new Seed("Vivência", "#b7791f", "home"),
            new Seed("Emergência", "#c2533f", "life-buoy"));

    /** Ciclos cujos gastos entram na meta: o que precisa estar pago antes de viajar (V12 faz o mesmo para os antigos). */
    static final Set<String> GOAL_CYCLES = Set.of("Preparação", "Imigração");

    static final List<Seed> CATEGORIES = List.of(
            new Seed("Agência", "#5b5f97", "briefcase"),
            new Seed("Documentos", "#6b7280", "file-text"),
            new Seed("Proficiência", "#7c3aed", "languages"),
            new Seed("Candidatura", "#2563eb", "send"),
            new Seed("Visto", "#0f766e", "stamp"),
            new Seed("Universidade", "#1f4d3f", "graduation-cap"),
            // Viagem: ida para a Alemanha e itens necessários para ela
            new Seed("Viagem", "#0891b2", "plane"),
            new Seed("Moradia", "#b45309", "home"),
            // Transporte: locomoção dentro do país (bicicleta, Deutschlandticket)
            new Seed("Transporte", "#4d7c0f", "bike"),
            new Seed("Saúde", "#dc2626", "heart-pulse"),
            new Seed("Alimentação", "#ea580c", "utensils"),
            new Seed("Higiene", "#0d9488", "sparkles"),
            new Seed("Pessoal", "#db2777", "user"),
            new Seed("Emergência", "#c2533f", "life-buoy"));

    private final ExpenseCategoryRepository categories;
    private final ExpenseCycleRepository cycles;

    public ExpenseCatalogSeeder(ExpenseCategoryRepository categories, ExpenseCycleRepository cycles) {
        this.categories = categories;
        this.cycles = cycles;
    }

    @EventListener
    public void onUserRegistered(UserRegisteredEvent event) {
        seedFor(event.userId());
    }

    /** Idempotente: só semeia o que o usuário ainda não tem nenhum. */
    @Transactional
    public void seedFor(UUID userId) {
        if (!cycles.existsByUserId(userId)) {
            for (int i = 0; i < CYCLES.size(); i++) {
                Seed s = CYCLES.get(i);
                ExpenseCycle cycle = new ExpenseCycle(userId, s.name(), s.color(), s.icon(), i + 1);
                cycle.setInGoal(GOAL_CYCLES.contains(s.name()));
                cycles.save(cycle);
            }
        }
        if (!categories.existsByUserId(userId)) {
            for (int i = 0; i < CATEGORIES.size(); i++) {
                Seed s = CATEGORIES.get(i);
                categories.save(new ExpenseCategory(userId, s.name(), s.color(), s.icon(), i + 1));
            }
        }
    }
}
