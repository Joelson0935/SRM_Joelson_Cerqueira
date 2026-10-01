package com.srm.creditengine.repository;

import com.srm.creditengine.domain.entity.Settlement;
import com.srm.creditengine.domain.enums.PaymentCurrency;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    /**
     * Verifica idempotência: retorna a liquidação existente para a key informada.
     */
    Optional<Settlement> findByIdempotencyKey(String idempotencyKey);

    /**
     * Extrato com filtros combinados por período, cedente e moeda.
     * Usa JOIN para acessar cedente via receivable — paginação server-side.
     */
    @Query("""
            SELECT s FROM Settlement s
            JOIN s.receivable r
            WHERE (:from IS NULL OR s.settledAt >= :from)
              AND (:to IS NULL OR s.settledAt <= :to)
              AND (:cedente IS NULL OR r.cedente = :cedente)
              AND (:currency IS NULL OR s.paymentCurrency = :currency)
            """)
    Page<Settlement> findByFilters(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("cedente") String cedente,
            @Param("currency") PaymentCurrency currency,
            Pageable pageable);
}
