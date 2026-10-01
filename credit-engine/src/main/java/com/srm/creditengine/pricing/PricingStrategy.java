package com.srm.creditengine.pricing;

import com.srm.creditengine.domain.enums.ReceivableType;

import java.math.BigDecimal;

/**
 * Contrato do motor de precificação.
 *
 * Cada tipo de recebível implementa esta interface fornecendo seu spread mensal.
 * O cálculo do valor presente é centralizado no PricingService — as strategies
 * são responsáveis apenas por fornecer os parâmetros específicos do tipo.
 *
 * Para adicionar um novo tipo: implemente esta interface e registre no PricingStrategyFactory.
 * Nenhum código existente precisa ser alterado (Open/Closed Principle).
 */
public interface PricingStrategy {

    /**
     * Spread mensal do tipo de recebível em decimal.
     * Ex: 1,5% a.m. → retorna new BigDecimal("0.015")
     */
    BigDecimal getSpread();

    /**
     * Tipo de recebível associado a esta strategy.
     * Usado pelo PricingStrategyFactory para resolver a strategy correta.
     */
    ReceivableType getReceivableType();
}
