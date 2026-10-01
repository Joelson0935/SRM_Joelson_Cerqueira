package com.srm.creditengine.pricing;

import com.srm.creditengine.domain.enums.ReceivableType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Strategy de precificação para Cheque Pré-datado.
 * Spread: 2,5% a.m. = 0.025
 */
@Component
public class ChequePreDatadoStrategy implements PricingStrategy {

    private static final BigDecimal SPREAD = new BigDecimal("0.025");

    @Override
    public BigDecimal getSpread() {
        return SPREAD;
    }

    @Override
    public ReceivableType getReceivableType() {
        return ReceivableType.CHEQUE_PRE_DATADO;
    }
}
