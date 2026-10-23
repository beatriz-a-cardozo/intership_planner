package com.interchangeplanner.expense.application;

import com.interchangeplanner.expense.api.CatalogRequest;
import com.interchangeplanner.expense.domain.ExpenseCycle;
import com.interchangeplanner.expense.domain.ExpenseCycleRepository;
import com.interchangeplanner.expense.domain.ExpenseRepository;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExpenseCycleService extends CatalogService<ExpenseCycle> {

    public ExpenseCycleService(ExpenseCycleRepository repository, ExpenseRepository expenses) {
        super(repository, ExpenseCycle::new, expenses::existsByCycleId, "Ciclo");
    }

    @Override
    protected void applyExtras(ExpenseCycle cycle, CatalogRequest request) {
        if (request.inGoal() != null) {
            cycle.setInGoal(request.inGoal());
        }
    }

    /** Ciclos cujos gastos entram na meta. Vazio = nenhum ciclo marcado (a meta considera todos os gastos). */
    @Transactional(readOnly = true)
    public Set<UUID> goalCycleIds(UUID userId) {
        return entities(userId).stream().filter(ExpenseCycle::isInGoal).map(ExpenseCycle::getId)
                .collect(Collectors.toSet());
    }
}
