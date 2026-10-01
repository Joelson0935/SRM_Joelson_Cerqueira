package com.srm.creditengine.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Propriedades de precificação lidas do application.yml.
 *
 * srm.pricing.base-rate: taxa base mensal do fundo (default: 0.01 = 1,00% a.m.)
 *
 * Intencionalmente separada do código de negócio para permitir
 * ajuste da taxa sem recompilação.
 */
@Component
@ConfigurationProperties(prefix = "srm.pricing")
public class PricingProperties {

    private BigDecimal baseRate = new BigDecimal("0.01");

    public BigDecimal getBaseRate() {
        return baseRate;
    }

    public void setBaseRate(BigDecimal baseRate) {
        this.baseRate = baseRate;
    }
}
