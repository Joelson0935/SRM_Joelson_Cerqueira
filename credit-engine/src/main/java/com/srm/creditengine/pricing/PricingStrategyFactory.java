package com.srm.creditengine.pricing;

import com.srm.creditengine.domain.enums.ReceivableType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Resolve a PricingStrategy correta dado um ReceivableType.
 *
 * O Spring injeta automaticamente todas as implementações de PricingStrategy
 * disponíveis no contexto. Adicionar um novo tipo não requer alteração aqui —
 * basta criar um novo @Component que implemente PricingStrategy.
 */
@Component
public class PricingStrategyFactory {

    private final Map<ReceivableType, PricingStrategy> strategies;

    public PricingStrategyFactory(List<PricingStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(PricingStrategy::getReceivableType, Function.identity()));
    }

    /**
     * Retorna a strategy para o tipo informado.
     *
     * @throws IllegalArgumentException se o tipo não tiver strategy registrada
     */
    public PricingStrategy getStrategy(ReceivableType type) {
        PricingStrategy strategy = strategies.get(type);
        if (strategy == null) {
            throw new IllegalArgumentException(
                    "No pricing strategy found for receivable type: " + type);
        }
        return strategy;
    }
}
