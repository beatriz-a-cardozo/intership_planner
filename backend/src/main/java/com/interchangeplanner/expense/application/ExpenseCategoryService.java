package com.interchangeplanner.expense.application;

import com.interchangeplanner.expense.domain.ExpenseCategory;
import com.interchangeplanner.expense.domain.ExpenseCategoryRepository;
import com.interchangeplanner.expense.domain.ExpenseRepository;
import org.springframework.stereotype.Service;

@Service
public class ExpenseCategoryService extends CatalogService<ExpenseCategory> {

    public ExpenseCategoryService(ExpenseCategoryRepository repository, ExpenseRepository expenses) {
        super(repository, ExpenseCategory::new, expenses::existsByCategoryId, "Categoria");
    }
}
