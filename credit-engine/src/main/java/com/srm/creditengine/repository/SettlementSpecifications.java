package com.srm.creditengine.repository;

import com.srm.creditengine.domain.entity.Settlement;
import com.srm.creditengine.domain.enums.PaymentCurrency;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

/**
 * Specifications para o extrato de liquidações.
 *
 * Cada método retorna um predicado para um filtro específico — ou null quando o
 * filtro não foi informado. Combinando-os via Specification.allOf, apenas os
 * filtros presentes entram na cláusula WHERE. Isso evita o padrão
 * ":param IS NULL OR coluna = :param", que no PostgreSQL com Hibernate 7 falha
 * com "could not determine data type of parameter" (SQLState 42P18) quando o
 * parâmetro é nulo.
 */
public final class SettlementSpecifications {

    private SettlementSpecifications() {
    }

    /** Liquidações com settledAt >= from. */
    public static Specification<Settlement> settledFrom(LocalDateTime from) {
        if (from == null) return null;
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("settledAt"), from);
    }

    /** Liquidações com settledAt <= to. */
    public static Specification<Settlement> settledTo(LocalDateTime to) {
        if (to == null) return null;
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("settledAt"), to);
    }

    /** Liquidações cujo recebível pertence ao cedente informado (JOIN). */
    public static Specification<Settlement> hasCedente(String cedente) {
        if (cedente == null || cedente.isBlank()) return null;
        return (root, query, cb) ->
                cb.equal(root.join("receivable", JoinType.INNER).get("cedente"), cedente);
    }

    /** Liquidações na moeda de pagamento informada. */
    public static Specification<Settlement> hasCurrency(PaymentCurrency currency) {
        if (currency == null) return null;
        return (root, query, cb) -> cb.equal(root.get("paymentCurrency"), currency);
    }

    /**
     * Combina todos os filtros. Specification.allOf ignora specs nulas, então
     * apenas os filtros efetivamente informados compõem a query.
     */
    public static Specification<Settlement> withFilters(LocalDateTime from,
                                                        LocalDateTime to,
                                                        String cedente,
                                                        PaymentCurrency currency) {
        return Specification.allOf(
                settledFrom(from),
                settledTo(to),
                hasCedente(cedente),
                hasCurrency(currency));
    }
}
