package com.interchangeplanner.exchange.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, UUID> {

    Optional<ExchangeRate> findFirstByBaseAndQuoteOrderByFetchedAtDesc(String base, String quote);

    boolean existsByBaseAndQuote(String base, String quote);

    /** Uma cotação por dia (a última buscada de cada data), em ordem cronológica. */
    @Query(nativeQuery = true, value = """
            SELECT DISTINCT ON (effective_date) *
            FROM exchange_rate
            WHERE base = :base AND quote = :quote AND effective_date BETWEEN :from AND :to
            ORDER BY effective_date, fetched_at DESC""")
    List<ExchangeRate> dailyHistory(@Param("base") String base, @Param("quote") String quote,
            @Param("from") LocalDate from, @Param("to") LocalDate to);
}
