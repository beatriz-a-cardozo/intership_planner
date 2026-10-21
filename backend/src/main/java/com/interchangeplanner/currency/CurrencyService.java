package com.interchangeplanner.currency;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CurrencyService {

    private final CurrencyRepository repository;

    public CurrencyService(CurrencyRepository repository) {
        this.repository = repository;
    }

    public List<CurrencyResponse> list() {
        return repository.findAllByOrderBySortOrderAsc().stream().map(CurrencyResponse::from).toList();
    }

    public boolean isSupported(String code) {
        return code != null && repository.existsById(code);
    }
}
