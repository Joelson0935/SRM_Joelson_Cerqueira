package com.srm.creditengine.exception;

/**
 * Lançada quando se tenta liquidar um recebível que já está com status SETTLED.
 * Mapeada para 409 Conflict pelo GlobalExceptionHandler (Passo 8).
 */
public class ReceivableAlreadySettledException extends RuntimeException {

    public ReceivableAlreadySettledException(Long receivableId) {
        super("Receivable " + receivableId + " has already been settled.");
    }
}
