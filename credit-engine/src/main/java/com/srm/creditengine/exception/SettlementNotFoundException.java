package com.srm.creditengine.exception;

/**
 * Lançada quando uma liquidação não é encontrada pelo ID informado.
 * Mapeada para 404 Not Found pelo GlobalExceptionHandler.
 */
public class SettlementNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

	public SettlementNotFoundException(Long id) {
        super("Settlement not found: " + id);
    }
}
