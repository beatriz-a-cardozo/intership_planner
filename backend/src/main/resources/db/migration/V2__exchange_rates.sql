-- =====================================================================
-- V2: cotações de câmbio (globais) e cotação manual por usuário
-- Guardamos só uma direção por par (ex.: EUR->BRL); o inverso é calculado.
-- =====================================================================

CREATE TABLE exchange_rate (
    id              uuid           PRIMARY KEY,
    base            char(3)        NOT NULL REFERENCES currency (code),
    quote           char(3)        NOT NULL REFERENCES currency (code),
    rate            numeric(19, 8) NOT NULL CHECK (rate > 0),
    -- provedor que forneceu a cotação (FRANKFURTER, AWESOMEAPI...)
    source          varchar(30)    NOT NULL,
    -- data de referência da cotação no provedor
    effective_date  date           NOT NULL,
    -- quando a aplicação buscou o valor (base do selo de "frescor")
    fetched_at      timestamptz    NOT NULL,
    CONSTRAINT ck_exchange_rate_pair CHECK (base <> quote)
);

CREATE INDEX idx_exchange_rate_pair_fetched ON exchange_rate (base, quote, fetched_at DESC);
CREATE INDEX idx_exchange_rate_pair_date ON exchange_rate (base, quote, effective_date);

CREATE TABLE user_exchange_rate_override (
    id          uuid           PRIMARY KEY,
    user_id     uuid           NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    base        char(3)        NOT NULL REFERENCES currency (code),
    quote       char(3)        NOT NULL REFERENCES currency (code),
    rate        numeric(19, 8) NOT NULL CHECK (rate > 0),
    active      boolean        NOT NULL DEFAULT true,
    created_at  timestamptz    NOT NULL,
    updated_at  timestamptz    NOT NULL,
    version     bigint         NOT NULL DEFAULT 0,
    CONSTRAINT uk_user_exchange_rate_override UNIQUE (user_id, base, quote),
    CONSTRAINT ck_user_exchange_rate_override_pair CHECK (base <> quote)
);
