package com.srm.creditengine.dto.response;

import com.srm.creditengine.domain.entity.ExchangeRate;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
