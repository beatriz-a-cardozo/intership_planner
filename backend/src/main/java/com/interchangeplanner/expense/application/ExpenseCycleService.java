package com.interchangeplanner.expense.application;

import com.interchangeplanner.expense.domain.ExpenseCycle;
import com.interchangeplanner.expense.domain.ExpenseCycleRepository;
import com.interchangeplanner.expense.domain.ExpenseRepository;
import org.springframework.stereotype.Service;

@Service
public class ExpenseCycleService extends CatalogService<ExpenseCycle> {

    public ExpenseCycleService(ExpenseCycleRepository repository, ExpenseRepository expenses) {
        super(repository, ExpenseCycle::new, expenses::existsByCycleId, "Ciclo");
    }
}
