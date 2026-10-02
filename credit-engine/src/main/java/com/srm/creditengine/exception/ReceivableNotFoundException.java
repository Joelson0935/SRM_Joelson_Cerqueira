package com.srm.creditengine.exception;

/**
 * Lançada quando um recebível não é encontrado pelo ID informado.
 * Mapeada para 404 Not Found pelo GlobalExceptionHandler (Passo 8).
 */
public class ReceivableNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

	public ReceivableNotFoundException(Long id) {
        super("Receivable not found: " + id);
    }
}
