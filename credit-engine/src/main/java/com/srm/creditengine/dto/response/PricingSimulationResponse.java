package com.srm.creditengine.dto.response;

import com.srm.creditengine.domain.enums.PaymentCurrency;
import com.srm.creditengine.domain.enums.ReceivableType;

import java.math.BigDecimal;

/**
 * Resposta da simulação de precificação (pré-liquidação).
 *
 * Permite ao operador visualizar o valor líquido antes de confirmar a liquidação.
 * Valores monetários retornados como BigDecimal com precisão total.
 */
public record PricingSimulationResponse(
        ReceivableType receivableType,
        BigDecimal faceValue,
        Integer termInMonths,
        PaymentCurrency paymentCurrency,
        BigDecimal presentValueBrl,
        BigDecimal finalAmount,
        PaymentCurrency finalCurrency,
        BigDecimal discountAmount,
        BigDecimal baseRateUsed,
        BigDecimal spreadUsed,
        BigDecimal exchangeRateUsed  // null quando BRL
) {}
