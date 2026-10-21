package com.interchangeplanner.expense.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface CatalogRepository<T extends CatalogItem> extends JpaRepository<T, UUID> {

    Optional<T> findByIdAndUserId(UUID id, UUID userId);

    List<T> findByUserIdOrderBySortOrderAscNameAsc(UUID userId);

    boolean existsByUserIdAndNameIgnoreCase(UUID userId, String name);

    boolean existsByUserIdAndNameIgnoreCaseAndIdNot(UUID userId, String name, UUID id);

    boolean existsByUserId(UUID userId);
}
