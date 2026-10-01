package com.srm.creditengine.repository;

import com.srm.creditengine.domain.entity.Receivable;
import com.srm.creditengine.domain.enums.ReceivableStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReceivableRepository extends JpaRepository<Receivable, Long> {

    /**
     * Busca recebível com lock pessimista para evitar liquidações concorrentes.
     * Usado exclusivamente pelo SettlementService dentro de uma transação.
     */
    @Query("SELECT r FROM Receivable r WHERE r.id = :id")
    Optional<Receivable> findByIdForUpdate(@Param("id") Long id);

    Page<Receivable> findByCedente(String cedente, Pageable pageable);

    Page<Receivable> findByStatus(ReceivableStatus status, Pageable pageable);
}
