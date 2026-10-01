package com.srm.creditengine.domain.enums;

/**
 * Tipos de recebível suportados pelo motor de precificação.
 * Cada tipo possui um spread mensal próprio definido na sua PricingStrategy.
 * Novos tipos são adicionados implementando PricingStrategy — sem alterar código existente.
 */
public enum ReceivableType {
    DUPLICATA_MERCANTIL,
    CHEQUE_PRE_DATADO
}
