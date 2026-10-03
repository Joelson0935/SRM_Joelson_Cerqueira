package com.srm.creditengine.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.srm.creditengine.domain.entity.Settlement;

@Repository
public interface SettlementRepository
        extends JpaRepository<Settlement, Long>, JpaSpecificationExecutor<Settlement> {

    /**
     * Verifica idempotência: retorna a liquidação existente para a key informada.
     */
    Optional<Settlement> findByIdempotencyKey(String idempotencyKey);

    // O extrato com filtros combinados é implementado via JpaSpecificationExecutor
    // (ver SettlementSpecifications). Specifications montam apenas os predicados
    // dos filtros presentes, evitando o padrão ":param IS NULL OR ..." que o
    // PostgreSQL rejeita por não conseguir inferir o tipo do parâmetro quando nulo
    // (SQLState 42P18, Hibernate 7+).
}
