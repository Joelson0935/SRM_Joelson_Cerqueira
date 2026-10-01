package com.srm.creditengine.dto.response;

import com.srm.creditengine.domain.entity.Receivable;
import com.srm.creditengine.domain.enums.PaymentCurrency;
import com.srm.creditengine.domain.enums.ReceivableStatus;
import com.srm.creditengine.domain.enums.ReceivableType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ReceivableResponse(
        Long id,
        String cedente,
        String documentNumber,
        ReceivableType type,
        BigDecimal faceValue,
        Integer termInMonths,
        LocalDate dueDate,
        PaymentCurrency paymentCurrency,
        ReceivableStatus status,
        LocalDateTime createdAt
) {
    public static ReceivableResponse from(Receivable receivable) {
        return new ReceivableResponse(
                receivable.getId(),
                receivable.getCedente(),
                receivable.getDocumentNumber(),
                receivable.getType(),
                receivable.getFaceValue(),
                receivable.getTermInMonths(),
                receivable.getDueDate(),
                receivable.getPaymentCurrency(),
                receivable.getStatus(),
                receivable.getCreatedAt()
        );
    }
}
