package com.srm.creditengine.domain.entity;

import com.srm.creditengine.domain.enums.PaymentCurrency;
import com.srm.creditengine.domain.enums.ReceivableType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Registro imutável de uma liquidação realizada.
 *
 * Todos os valores relevantes ao cálculo são gravados no momento da liquidação,
 * garantindo rastreabilidade completa independente de mudanças futuras nas
 * configurações (taxa base, spread, câmbio).
 *
 * Não existe endpoint de UPDATE ou DELETE para esta entidade.
 * Correções são tratadas como estorno (nova operação) — fora do escopo do MVP.
 */
@Entity
@Table(name = "settlements")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Recebível que originou esta liquidação.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receivable_id", nullable = false)
    private Receivable receivable;

    @Enumerated(EnumType.STRING)
    @Column(name = "receivable_type", nullable = false, length = 30)
    private ReceivableType receivableType;

    /**
     * Valor de face original do recebível no momento da liquidação.
     */
    @Column(name = "face_value", nullable = false, precision = 19, scale = 6)
    private BigDecimal faceValue;

    /**
     * Valor presente calculado em BRL (antes da conversão cambial).
     */
    @Column(name = "present_value_brl", nullable = false, precision = 19, scale = 6)
    private BigDecimal presentValueBrl;

    /**
     * Valor final liquidado na moeda de pagamento.
     * Igual ao present_value_brl quando currency = BRL.
     * Igual ao present_value_brl / exchange_rate quando currency = USD.
     */
    @Column(name = "final_amount", nullable = false, precision = 19, scale = 6)
    private BigDecimal finalAmount;

    /**
     * Deságio = face_value - present_value_brl (sempre em BRL).
     */
    @Column(name = "discount_amount", nullable = false, precision = 19, scale = 6)
    private BigDecimal discountAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_currency", nullable = false, length = 10)
    private PaymentCurrency paymentCurrency;

    /**
     * Taxa base utilizada no cálculo — gravada para auditoria.
     */
    @Column(name = "base_rate_used", nullable = false, precision = 10, scale = 6)
    private BigDecimal baseRateUsed;

    /**
     * Spread do tipo de recebível utilizado no cálculo — gravado para auditoria.
     */
    @Column(name = "spread_used", nullable = false, precision = 10, scale = 6)
    private BigDecimal spreadUsed;

    /**
     * Prazo em meses utilizado no cálculo.
     */
    @Column(name = "term_in_months_used", nullable = false)
    private Integer termInMonthsUsed;

    /**
     * Taxa de câmbio efetivamente usada na conversão.
     * NULL quando payment_currency = BRL.
     */
    @Column(name = "exchange_rate_used", precision = 19, scale = 8)
    private BigDecimal exchangeRateUsed;

    /**
     * Chave de idempotência enviada pelo cliente.
     * Unique constraint no banco garante que a mesma key não gere duas liquidações.
     */
    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    /**
     * Timestamp da liquidação — imutável após criação.
     */
    @Column(name = "settled_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime settledAt = LocalDateTime.now();
}
