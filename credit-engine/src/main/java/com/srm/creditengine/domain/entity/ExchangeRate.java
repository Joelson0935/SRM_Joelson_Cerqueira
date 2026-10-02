package com.srm.creditengine.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Taxa de câmbio cadastrada manualmente pela mesa de operações.
 *
 * Cada registro representa uma taxa vigente a partir de effectiveAt.
 * A taxa mais recente com effectiveAt <= now() é a taxa corrente.
 *
 * Registros são imutáveis após criação — para atualizar a taxa,
 * cria-se um novo registro com effectiveAt mais recente.
 *
 * currencyPair: par de moedas no formato "USD/BRL"
 * rate: quantidade de BRL por 1 unidade da moeda estrangeira
 *       Ex: rate = 5.4321 significa 1 USD = 5.4321 BRL
 */
@Entity
@Table(name = "exchange_rates")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Par de moedas no formato "USD/BRL".
     */
    @Column(name = "currency_pair", nullable = false, length = 10)
    private String currencyPair;

    /**
     * Taxa de câmbio: unidades de BRL por 1 unidade da moeda estrangeira.
     * NUMERIC(19,8): 8 casas para precisão cambial adequada.
     */
    @Column(name = "rate", nullable = false, precision = 19, scale = 8)
    private BigDecimal rate;

    /**
     * Momento a partir do qual esta taxa entra em vigência.
     */
    @Column(name = "effective_at", nullable = false)
    private LocalDateTime effectiveAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
