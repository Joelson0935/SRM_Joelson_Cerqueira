package com.srm.creditengine.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.srm.creditengine.domain.entity.ExchangeRate;

/**
 * Resposta com os dados de uma taxa de câmbio.
 *
 * rate é retornado como BigDecimal — o Jackson serializa com precisão total.
 */
public record ExchangeRateResponse(
        Long id,
        String currencyPair,
        BigDecimal rate,
        LocalDateTime effectiveAt,
        LocalDateTime createdAt
) {
    public static ExchangeRateResponse from(ExchangeRate exchangeRate) {
        return new ExchangeRateResponse(
                exchangeRate.getId(),
                exchangeRate.getCurrencyPair(),
                exchangeRate.getRate(),
                exchangeRate.getEffectiveAt(),
                exchangeRate.getCreatedAt()
        );
    }
}
