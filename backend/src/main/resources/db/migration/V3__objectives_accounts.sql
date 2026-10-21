-- =====================================================================
-- V3: objetivos, contas e histórico de saldos
-- FKs compostas (id, user_id) garantem no próprio banco que uma conta só
-- se vincula a um objetivo do MESMO usuário.
-- =====================================================================

CREATE TABLE objective (
    id                      uuid            PRIMARY KEY,
    user_id                 uuid            NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    name                    varchar(120)    NOT NULL,
    description             varchar(1000),
    country_code            char(2),
    city                    varchar(120),
    start_date              date,
    target_date             date            NOT NULL,
    currency                char(3)         NOT NULL REFERENCES currency (code),
    target_mode             varchar(10)     NOT NULL DEFAULT 'DERIVED' CHECK (target_mode IN ('MANUAL', 'DERIVED')),
    target_amount           numeric(19, 4)  CHECK (target_amount >= 0),
    planned_monthly_saving  numeric(19, 4)  CHECK (planned_monthly_saving >= 0),
    status                  varchar(12)     NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'COMPLETED', 'ARCHIVED')),
    is_primary              boolean         NOT NULL DEFAULT false,
    color                   varchar(20),
    icon                    varchar(40),
    image_url               varchar(500),
    -- duração do objetivo em meses (mestrado = 24): base das ocorrências sugeridas dos gastos recorrentes
    horizon_months          smallint        NOT NULL DEFAULT 24 CHECK (horizon_months BETWEEN 1 AND 120),
    -- quais gastos compõem a meta derivada
    expense_scope           varchar(15)     NOT NULL DEFAULT 'REQUIRED_ONLY' CHECK (expense_scope IN ('REQUIRED_ONLY', 'ALL')),
    created_at              timestamptz     NOT NULL,
    updated_at              timestamptz     NOT NULL,
    version                 bigint          NOT NULL DEFAULT 0,
    CONSTRAINT uk_objective_id_user UNIQUE (id, user_id),
    CONSTRAINT ck_objective_manual_target CHECK (target_mode <> 'MANUAL' OR target_amount IS NOT NULL),
    CONSTRAINT ck_objective_dates CHECK (start_date IS NULL OR start_date <= target_date)
);

CREATE INDEX idx_objective_user_status ON objective (user_id, status);
-- no máximo um objetivo principal por usuário
CREATE UNIQUE INDEX uk_objective_primary ON objective (user_id) WHERE is_primary;

CREATE TABLE account (
    id            uuid         PRIMARY KEY,
    user_id       uuid         NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    objective_id  uuid,
    name          varchar(80)  NOT NULL,
    currency      char(3)      NOT NULL REFERENCES currency (code),
    type          varchar(15)  NOT NULL CHECK (type IN ('CHECKING', 'SAVINGS', 'INVESTMENT', 'CASH', 'BLOCKED', 'OTHER')),
    archived      boolean      NOT NULL DEFAULT false,
    created_at    timestamptz  NOT NULL,
    updated_at    timestamptz  NOT NULL,
    version       bigint       NOT NULL DEFAULT 0,
    CONSTRAINT uk_account_id_user UNIQUE (id, user_id),
    -- apagar o objetivo desvincula a conta (não apaga o dinheiro)
    CONSTRAINT fk_account_objective FOREIGN KEY (objective_id, user_id)
        REFERENCES objective (id, user_id) ON DELETE SET NULL (objective_id)
);

CREATE INDEX idx_account_user ON account (user_id);
CREATE INDEX idx_account_objective ON account (objective_id);

CREATE TABLE account_balance_snapshot (
    id          uuid            PRIMARY KEY,
    account_id  uuid            NOT NULL REFERENCES account (id) ON DELETE CASCADE,
    as_of       date            NOT NULL,
    balance     numeric(19, 4)  NOT NULL,
    created_at  timestamptz     NOT NULL,
    updated_at  timestamptz     NOT NULL,
    version     bigint          NOT NULL DEFAULT 0,
    -- um saldo por conta por dia (lançar de novo no mesmo dia substitui)
    CONSTRAINT uk_account_balance_snapshot UNIQUE (account_id, as_of)
);
