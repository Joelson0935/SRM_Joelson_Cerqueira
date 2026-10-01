package com.srm.creditengine.pricing;

import java.math.BigDecimal;

/**
 * Resultado imutável do motor de precificação.
 *
 * presentValueBrl  → valor presente arredondado em BRL (half-even, 2 casas)
 * discountAmount   → deságio = faceValue - presentValueBrl (em BRL)
 * baseRateUsed     → taxa base utilizada no cálculo (para auditoria)
 * spreadUsed       → spread do tipo utilizado no cálculo (para auditoria)
 * termInMonths     → prazo utilizado no cálculo (para auditoria)
 */
public record PricingResult(
        BigDecimal presentValueBrl,
        BigDecimal discountAmount,
        BigDecimal baseRateUsed,
        BigDecimal spreadUsed,
        Integer termInMonths
) {}
