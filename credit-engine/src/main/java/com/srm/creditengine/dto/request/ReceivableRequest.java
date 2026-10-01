package com.srm.creditengine.dto.request;

import com.srm.creditengine.domain.enums.PaymentCurrency;
import com.srm.creditengine.domain.enums.ReceivableType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request para cadastro de um recebível.
 */
public record ReceivableRequest(

        @NotBlank(message = "Cedente is required")
        String cedente,

        @NotBlank(message = "Document number is required")
        String documentNumber,

        @NotNull(message = "Receivable type is required")
        ReceivableType type,

        @NotNull(message = "Face value is required")
        @DecimalMin(value = "0.01", message = "Face value must be positive")
        BigDecimal faceValue,

        @NotNull(message = "Term in months is required")
        @Min(value = 1, message = "Term must be at least 1 month")
        @Max(value = 360, message = "Term cannot exceed 360 months")
        Integer termInMonths,

        @NotNull(message = "Due date is required")
        @Future(message = "Due date must be in the future")
        LocalDate dueDate,

        @NotNull(message = "Payment currency is required")
        PaymentCurrency paymentCurrency
) {}
