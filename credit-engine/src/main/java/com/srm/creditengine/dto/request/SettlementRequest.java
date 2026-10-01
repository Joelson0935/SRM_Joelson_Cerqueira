package com.srm.creditengine.dto.request;

import com.srm.creditengine.domain.enums.PaymentCurrency;
import jakarta.validation.constraints.NotNull;

/**
 * Request para liquidação de um recebível.
 *
 * A idempotency key é lida do header HTTP "Idempotency-Key" no controller,
 * não faz parte do body — isso segue o padrão de APIs financeiras (Stripe, etc).
 */
public record SettlementRequest(

        @NotNull(message = "Receivable ID is required")
        Long receivableId,

        @NotNull(message = "Payment currency is required")
        PaymentCurrency currency
) {}
