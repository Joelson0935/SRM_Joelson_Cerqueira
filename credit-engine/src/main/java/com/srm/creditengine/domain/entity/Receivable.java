package com.srm.creditengine.domain.entity;

import com.srm.creditengine.domain.enums.PaymentCurrency;
import com.srm.creditengine.domain.enums.ReceivableStatus;
import com.srm.creditengine.domain.enums.ReceivableType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Recebível cadastrado no sistema para precificação e liquidação.
 *
 * face_value e term são os dados de entrada do motor de precificação.
 * status controla o ciclo de vida: PENDING → SETTLED (estado terminal).
 *
 * NUMERIC(19,6) garante precisão decimal exata — sem ponto flutuante binário.
 */
@Entity
@Table(name = "receivables")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Receivable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cedente", nullable = false, length = 255)
    private String cedente;

    @Column(name = "document_number", nullable = false, length = 100)
    private String documentNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private ReceivableType type;

    /**
     * Valor de face (nominal) do recebível em BRL.
     * NUMERIC(19,6): precisão suficiente para cálculos intermediários.
     */
    @Column(name = "face_value", nullable = false, precision = 19, scale = 6)
    private BigDecimal faceValue;

    /**
     * Prazo em meses inteiros até o vencimento.
     */
    @Column(name = "term_in_months", nullable = false)
    private Integer termInMonths;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_currency", nullable = false, length = 10)
    private PaymentCurrency paymentCurrency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ReceivableStatus status = ReceivableStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * Marca o recebível como liquidado.
     * Chamado exclusivamente pelo SettlementService dentro de uma transação.
     */
    public void settle() {
        if (this.status == ReceivableStatus.SETTLED) {
            throw new IllegalStateException(
                "Receivable " + this.id + " is already settled.");
        }
        this.status = ReceivableStatus.SETTLED;
    }
}
