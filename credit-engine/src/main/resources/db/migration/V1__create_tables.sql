-- =============================================================================
-- V1__create_tables.sql
-- Criação das tabelas principais do SRM Credit Engine
-- =============================================================================

-- -----------------------------------------------------------------------------
-- receivables: recebíveis cadastrados para precificação e liquidação
-- -----------------------------------------------------------------------------
CREATE TABLE receivables (
    id               BIGSERIAL       PRIMARY KEY,
    cedente          VARCHAR(255)    NOT NULL,
    document_number  VARCHAR(100)    NOT NULL,
    type             VARCHAR(30)     NOT NULL,
    face_value       NUMERIC(19, 6)  NOT NULL,
    term_in_months   INTEGER         NOT NULL,
    due_date         DATE            NOT NULL,
    payment_currency VARCHAR(10)     NOT NULL,
    status           VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    created_at       TIMESTAMP       NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_receivables_type
        CHECK (type IN ('DUPLICATA_MERCANTIL', 'CHEQUE_PRE_DATADO')),

    CONSTRAINT chk_receivables_currency
        CHECK (payment_currency IN ('BRL', 'USD')),

    CONSTRAINT chk_receivables_status
        CHECK (status IN ('PENDING', 'SETTLED')),

    CONSTRAINT chk_receivables_face_value
        CHECK (face_value > 0),

    CONSTRAINT chk_receivables_term
        CHECK (term_in_months > 0)
);

-- Índice para busca por cedente (usado no extrato)
CREATE INDEX idx_receivables_cedente ON receivables (cedente);

-- Índice para busca por status (listagem de pendentes)
CREATE INDEX idx_receivables_status ON receivables (status);

-- -----------------------------------------------------------------------------
-- exchange_rates: taxas de câmbio com vigência temporal
-- -----------------------------------------------------------------------------
CREATE TABLE exchange_rates (
    id              BIGSERIAL       PRIMARY KEY,
    currency_pair   VARCHAR(10)     NOT NULL,
    rate            NUMERIC(19, 8)  NOT NULL,
    effective_at    TIMESTAMP       NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_exchange_rates_rate
        CHECK (rate > 0)
);

-- Índice para busca da taxa vigente mais recente por par de moedas
CREATE INDEX idx_exchange_rates_pair_effective
    ON exchange_rates (currency_pair, effective_at DESC);

-- -----------------------------------------------------------------------------
-- settlements: registros imutáveis de liquidação (auditoria)
-- -----------------------------------------------------------------------------
CREATE TABLE settlements (
    id                   BIGSERIAL       PRIMARY KEY,
    receivable_id        BIGINT          NOT NULL REFERENCES receivables (id),
    receivable_type      VARCHAR(30)     NOT NULL,
    face_value           NUMERIC(19, 6)  NOT NULL,
    present_value_brl    NUMERIC(19, 6)  NOT NULL,
    final_amount         NUMERIC(19, 6)  NOT NULL,
    discount_amount      NUMERIC(19, 6)  NOT NULL,
    payment_currency     VARCHAR(10)     NOT NULL,
    base_rate_used       NUMERIC(10, 6)  NOT NULL,
    spread_used          NUMERIC(10, 6)  NOT NULL,
    term_in_months_used  INTEGER         NOT NULL,
    exchange_rate_used   NUMERIC(19, 8),
    idempotency_key      VARCHAR(100)    NOT NULL,
    settled_at           TIMESTAMP       NOT NULL DEFAULT NOW(),

    -- Garante idempotência: mesma key nunca gera dois registros
    CONSTRAINT uq_settlements_idempotency_key
        UNIQUE (idempotency_key),

    -- Garante que um recebível nunca é liquidado duas vezes
    CONSTRAINT uq_settlements_receivable_id
        UNIQUE (receivable_id),

    CONSTRAINT chk_settlements_currency
        CHECK (payment_currency IN ('BRL', 'USD')),

    CONSTRAINT chk_settlements_final_amount
        CHECK (final_amount > 0)
);

-- Índice para extrato por período
CREATE INDEX idx_settlements_settled_at ON settlements (settled_at);

-- Índice para extrato por moeda
CREATE INDEX idx_settlements_currency ON settlements (payment_currency);
