package com.srm.creditengine.exception;

/**
 * Lançada quando não há taxa de câmbio vigente para o par de moedas solicitado.
 * Mapeada para 404 Not Found pelo GlobalExceptionHandler (Passo 8).
 */
public class ExchangeRateNotFoundException extends RuntimeException {

    public ExchangeRateNotFoundException(String currencyPair) {
        super("No exchange rate found for currency pair: " + currencyPair);
    }
}
