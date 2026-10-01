package com.srm.creditengine.domain.enums;

/**
 * Ciclo de vida de um recebível.
 *
 * PENDING → recebível cadastrado, aguardando liquidação.
 * SETTLED → recebível liquidado. Estado terminal — não pode retornar a PENDING.
 */
public enum ReceivableStatus {
    PENDING,
    SETTLED
}
