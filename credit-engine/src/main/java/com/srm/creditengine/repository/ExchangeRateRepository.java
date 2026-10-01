package com.srm.creditengine.repository;

import com.srm.creditengine.domain.entity.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {

    /**
     * Retorna a taxa mais recente para o par de moedas vigente em um dado momento.
     * Usado na liquidação: busca a taxa cuja effectiveAt seja a maior <= referenceTime.
     */
    @Query("""
            SELECT e FROM ExchangeRate e
            WHERE e.currencyPair = :currencyPair
              AND e.effectiveAt <= :referenceTime
            ORDER BY e.effectiveAt DESC
            LIMIT 1
            """)
    Optional<ExchangeRate> findLatestByCurrencyPairAt(
            @Param("currencyPair") String currencyPair,
            @Param("referenceTime") LocalDateTime referenceTime);
}
