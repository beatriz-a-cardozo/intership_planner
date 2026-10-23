package com.interchangeplanner.luggage.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LuggageBagRepository extends JpaRepository<LuggageBag, UUID> {

    Optional<LuggageBag> findByIdAndUserId(UUID id, UUID userId);

    List<LuggageBag> findByUserIdOrderBySortOrderAscCreatedAtAsc(UUID userId);
}
