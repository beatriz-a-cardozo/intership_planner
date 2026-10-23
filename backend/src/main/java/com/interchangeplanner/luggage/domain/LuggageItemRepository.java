package com.interchangeplanner.luggage.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LuggageItemRepository extends JpaRepository<LuggageItem, UUID> {

    Optional<LuggageItem> findByIdAndUserId(UUID id, UUID userId);

    List<LuggageItem> findByUserIdOrderByNameAsc(UUID userId);
}
