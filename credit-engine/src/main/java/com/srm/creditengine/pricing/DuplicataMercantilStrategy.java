package com.srm.creditengine.pricing;

import com.srm.creditengine.domain.enums.ReceivableType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Strategy de precificação para Duplicata Mercantil.
 * Spread: 1,5% a.m. = 0.015
 */
@Component
public class DuplicataMercantilStrategy implements PricingStrategy {

    private static final BigDecimal SPREAD = new BigDecimal("0.015");

    @Override
    public BigDecimal getSpread() {
        return SPREAD;
    }

    @Override
    public ReceivableType getReceivableType() {
        return ReceivableType.DUPLICATA_MERCANTIL;
    }
}
