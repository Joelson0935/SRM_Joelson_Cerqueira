package com.srm.creditengine.service;

import com.srm.creditengine.config.PricingProperties;
import com.srm.creditengine.domain.enums.ReceivableType;
import com.srm.creditengine.pricing.PricingResult;
import com.srm.creditengine.pricing.PricingStrategy;
import com.srm.creditengine.pricing.PricingStrategyFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Motor de precificação de recebíveis.
 *
 * Fórmula:
 *   Valor Presente = Valor de Face / (1 + Taxa Base + Spread) ^ Prazo
 *
 * Regras de precisão (definidas no SPEC.md):
 * - Todos os cálculos intermediários usam MathContext de 20 dígitos significativos.
 * - BigDecimal.pow() aceita apenas expoentes inteiros — correto para prazo em meses.
 * - Arredondamento HALF_EVEN aplicado UMA ÚNICA VEZ ao resultado final em BRL.
 * - float/double não são usados em nenhuma etapa.
 */
@Service
public class PricingService {

    /**
     * Precisão para cálculos intermediários — 20 dígitos significativos
     * eliminam qualquer acúmulo de erro antes do arredondamento final.
     */
    private static final MathContext MATH_CONTEXT = new MathContext(20, RoundingMode.HALF_EVEN);

    /**
     * Escala final para valores monetários em BRL: 2 casas, half-even.
     */
    private static final int MONETARY_SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_EVEN;

    private final PricingStrategyFactory strategyFactory;
    private final PricingProperties pricingProperties;

    public PricingService(PricingStrategyFactory strategyFactory,
                          PricingProperties pricingProperties) {
        this.strategyFactory = strategyFactory;
        this.pricingProperties = pricingProperties;
    }

    /**
     * Calcula o valor presente de um recebível.
     *
     * @param faceValue     valor de face em BRL (deve ser positivo)
     * @param termInMonths  prazo em meses inteiros (deve ser positivo)
     * @param type          tipo do recebível (determina o spread)
     * @return PricingResult com VP arredondado, deságio e parâmetros usados
     */
    public PricingResult calculate(BigDecimal faceValue, Integer termInMonths, ReceivableType type) {
        PricingStrategy strategy = strategyFactory.getStrategy(type);

        BigDecimal baseRate = pricingProperties.getBaseRate();
        BigDecimal spread   = strategy.getSpread();

        // divisor = (1 + baseRate + spread) ^ termInMonths
        // Calculado com alta precisão antes de qualquer arredondamento
        BigDecimal divisor = BigDecimal.ONE
                .add(baseRate, MATH_CONTEXT)
                .add(spread, MATH_CONTEXT)
                .pow(termInMonths, MATH_CONTEXT);

        // VP intermediário — precisão total, sem arredondamento
        BigDecimal presentValueIntermediate = faceValue.divide(divisor, MATH_CONTEXT);

        // Arredondamento HALF_EVEN aplicado UMA ÚNICA VEZ ao resultado final em BRL
        BigDecimal presentValueBrl = presentValueIntermediate
                .setScale(MONETARY_SCALE, ROUNDING_MODE);

        // Deságio = faceValue - VP arredondado (também arredondado para 2 casas)
        BigDecimal discountAmount = faceValue
                .subtract(presentValueBrl)
                .setScale(MONETARY_SCALE, ROUNDING_MODE);

        return new PricingResult(
                presentValueBrl,
                discountAmount,
                baseRate,
                spread,
                termInMonths
        );
    }
}
