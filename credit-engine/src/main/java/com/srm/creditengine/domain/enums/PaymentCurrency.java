package com.srm.creditengine.domain.enums;

/**
 * Moedas suportadas para liquidação.
 * BRL: liquidação em reais (sem conversão cambial).
 * USD: liquidação em dólares (aplica conversão sobre o VP em BRL).
 */
public enum PaymentCurrency {
    BRL,
    USD
}
