package com.srm.creditengine.dto.request;

import com.srm.creditengine.domain.enums.PaymentCurrency;
import com.srm.creditengine.domain.enums.ReceivableType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Request para simulação de precificação (sem persistência).
 * Usado pelo frontend para exibir o VP em tempo real antes de liquidar.
 */
public record SimulationRequest(

        @NotNull(message = "Receivable type is required")
        ReceivableType type,

        @NotNull(message = "Face value is required")
        @DecimalMin(value = "0.01", message = "Face value must be positive")
        BigDecimal faceValue,

        @NotNull(message = "Term in months is required")
        @Min(value = 1, message = "Term must be at least 1 month")
        @Max(value = 360, message = "Term cannot exceed 360 months")
        Integer termInMonths,

        @NotNull(message = "Payment currency is required")
        PaymentCurrency paymentCurrency
) {}
