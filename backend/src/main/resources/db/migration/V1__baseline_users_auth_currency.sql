-- =====================================================================
-- V1: usuários, configurações, refresh tokens e moedas
-- Convenções do projeto:
--   * PK uuid gerado pela aplicação (UUIDv7, ordenável no tempo)
--   * created_at / updated_at em timestamptz (UTC)
--   * enums como varchar + CHECK (evoluem sem ALTER TYPE)
--   * dinheiro em numeric(19,4) (tabelas das próximas fases)
-- =====================================================================

CREATE TABLE currency (
    code        char(3)      PRIMARY KEY,
    name        varchar(60)  NOT NULL,
    symbol      varchar(8)   NOT NULL,
    scale       smallint     NOT NULL DEFAULT 2 CHECK (scale BETWEEN 0 AND 4),
    sort_order  smallint     NOT NULL DEFAULT 0
);

INSERT INTO currency (code, name, symbol, scale, sort_order) VALUES
    ('BRL', 'Real brasileiro', 'R$', 2, 1),
    ('EUR', 'Euro', '€', 2, 2);

CREATE TABLE app_user (
    id                       uuid          PRIMARY KEY,
    -- e-mail sempre normalizado (trim + minúsculas) pela aplicação
    email                    varchar(254)  NOT NULL,
    password_hash            varchar(100),
    display_name             varchar(80)   NOT NULL,
    auth_provider            varchar(20)   NOT NULL DEFAULT 'LOCAL'
                             CHECK (auth_provider IN ('LOCAL', 'GOOGLE', 'GITHUB')),
    onboarding_completed_at  timestamptz,
    created_at               timestamptz   NOT NULL,
    updated_at               timestamptz   NOT NULL,
    version                  bigint        NOT NULL DEFAULT 0,
    CONSTRAINT uk_app_user_email UNIQUE (email),
    CONSTRAINT ck_app_user_email_normalized CHECK (email = lower(btrim(email))),
    -- conta local exige senha; contas OAuth (futuro) não
    CONSTRAINT ck_app_user_password CHECK (auth_provider <> 'LOCAL' OR password_hash IS NOT NULL)
);

CREATE TABLE user_settings (
    user_id             uuid         PRIMARY KEY REFERENCES app_user (id) ON DELETE CASCADE,
    theme               varchar(10)  NOT NULL DEFAULT 'SYSTEM' CHECK (theme IN ('LIGHT', 'DARK', 'SYSTEM')),
    locale              varchar(10)  NOT NULL DEFAULT 'pt-BR',
    timezone            varchar(60)  NOT NULL DEFAULT 'America/Sao_Paulo',
    primary_currency    char(3)      NOT NULL DEFAULT 'BRL' REFERENCES currency (code),
    secondary_currency  char(3)      NOT NULL DEFAULT 'EUR' REFERENCES currency (code),
    created_at          timestamptz  NOT NULL,
    updated_at          timestamptz  NOT NULL,
    version             bigint       NOT NULL DEFAULT 0,
    CONSTRAINT ck_user_settings_currencies CHECK (primary_currency <> secondary_currency)
);

CREATE TABLE refresh_token (
    id           uuid          PRIMARY KEY,
    user_id      uuid          NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    -- só o hash SHA-256 do token é armazenado; o valor real fica apenas no cookie do navegador
    token_hash   char(64)      NOT NULL,
    -- tokens rotacionados a partir do mesmo login compartilham a família (detecção de reuso)
    family_id    uuid          NOT NULL,
    expires_at   timestamptz   NOT NULL,
    revoked_at   timestamptz,
    created_at   timestamptz   NOT NULL,
    updated_at   timestamptz   NOT NULL,
    version      bigint        NOT NULL DEFAULT 0,
    CONSTRAINT uk_refresh_token_hash UNIQUE (token_hash)
);

CREATE INDEX idx_refresh_token_user ON refresh_token (user_id);
CREATE INDEX idx_refresh_token_family ON refresh_token (family_id);
CREATE INDEX idx_refresh_token_expires ON refresh_token (expires_at);
