package com.srm.creditengine.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Request para cadastro de uma nova taxa de câmbio.
 *
 * currencyPair: par no formato "USD/BRL"
 * rate: unidades de BRL por 1 unidade da moeda estrangeira (deve ser positivo)
 * effectiveAt: momento a partir do qual a taxa entra em vigência (default: now se omitido)
 */
public record ExchangeRateRequest(

        @NotBlank(message = "Currency pair is required")
        @Pattern(regexp = "^[A-Z]{3}/[A-Z]{3}$", message = "Currency pair must be in format 'USD/BRL'")
        String currencyPair,

        @NotNull(message = "Rate is required")
        @DecimalMin(value = "0.00000001", message = "Rate must be positive")
        BigDecimal rate,

        LocalDateTime effectiveAt
) {}
