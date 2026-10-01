package com.srm.creditengine.dto.response;

import com.srm.creditengine.domain.entity.Settlement;
import com.srm.creditengine.domain.enums.PaymentCurrency;
import com.srm.creditengine.domain.enums.ReceivableType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Resposta completa de uma liquidação.
 *
 * Inclui todos os parâmetros usados no cálculo para rastreabilidade completa.
 * Retornada tanto em novas liquidações (201) quanto em retentativas idempotentes (200).
 */
public record SettlementResponse(
        Long id,
        Long receivableId,
        ReceivableType receivableType,
        BigDecimal faceValue,
        BigDecimal presentValueBrl,
        BigDecimal finalAmount,
        BigDecimal discountAmount,
        PaymentCurrency paymentCurrency,
        BigDecimal baseRateUsed,
        BigDecimal spreadUsed,
        Integer termInMonthsUsed,
        BigDecimal exchangeRateUsed,
        String idempotencyKey,
        LocalDateTime settledAt
) {
    public static SettlementResponse from(Settlement settlement) {
        return new SettlementResponse(
                settlement.getId(),
                settlement.getReceivable().getId(),
                settlement.getReceivableType(),
                settlement.getFaceValue(),
                settlement.getPresentValueBrl(),
                settlement.getFinalAmount(),
                settlement.getDiscountAmount(),
                settlement.getPaymentCurrency(),
                settlement.getBaseRateUsed(),
                settlement.getSpreadUsed(),
                settlement.getTermInMonthsUsed(),
                settlement.getExchangeRateUsed(),
                settlement.getIdempotencyKey(),
                settlement.getSettledAt()
        );
    }
}
