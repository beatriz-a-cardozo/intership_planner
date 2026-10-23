package com.interchangeplanner.notification.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExchangeRateAlertRepository extends JpaRepository<ExchangeRateAlert, UUID> {

    List<ExchangeRateAlert> findByUserIdOrderByCreatedAtAsc(UUID userId);

    Optional<ExchangeRateAlert> findByIdAndUserId(UUID id, UUID userId);

    List<ExchangeRateAlert> findByActiveTrue();

    long countByUserId(UUID userId);
}
