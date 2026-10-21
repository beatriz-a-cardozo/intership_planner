package com.interchangeplanner.currency;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CurrencyRepository extends JpaRepository<Currency, String> {

    List<Currency> findAllByOrderBySortOrderAsc();
}
