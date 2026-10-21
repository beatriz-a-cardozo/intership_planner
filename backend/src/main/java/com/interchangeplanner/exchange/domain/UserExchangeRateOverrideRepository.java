package com.interchangeplanner.exchange.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserExchangeRateOverrideRepository extends JpaRepository<UserExchangeRateOverride, UUID> {

    Optional<UserExchangeRateOverride> findByUserIdAndBaseAndQuote(UUID userId, String base, String quote);

    List<UserExchangeRateOverride> findByUserIdAndActiveTrue(UUID userId);
}
