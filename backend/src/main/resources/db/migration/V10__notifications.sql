-- =====================================================================
-- V10: notificações in-app, preferências por categoria, alertas de câmbio com histerese e ShedLock.
-- A deduplicação é do banco: (user_id, dedupe_key) único e insert com ON CONFLICT DO NOTHING.
-- =====================================================================

CREATE TABLE notification (
    id           uuid           PRIMARY KEY,
    user_id      uuid           NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    category     varchar(20)    NOT NULL
                 CHECK (category IN ('DEADLINES', 'PAYMENTS', 'RESPONSES', 'TASKS', 'EXCHANGE', 'MOTIVATIONAL')),
    severity     varchar(10)    NOT NULL DEFAULT 'INFO' CHECK (severity IN ('INFO', 'WARNING', 'CRITICAL')),
    title        varchar(200)   NOT NULL,
    body         varchar(1000),
    action_url   varchar(300),
    source_type  varchar(30),
    source_id    uuid,
    dedupe_key   varchar(200)   NOT NULL,
    read_at      timestamptz,
    created_at   timestamptz    NOT NULL,
    updated_at   timestamptz    NOT NULL,
    version      bigint         NOT NULL DEFAULT 0,
    CONSTRAINT uk_notification_dedupe UNIQUE (user_id, dedupe_key)
);
CREATE INDEX idx_notification_user_unread ON notification (user_id, read_at, created_at DESC);

CREATE TABLE notification_preference (
    id          uuid          PRIMARY KEY,
    user_id     uuid          NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    category    varchar(20)   NOT NULL
                CHECK (category IN ('DEADLINES', 'PAYMENTS', 'RESPONSES', 'TASKS', 'EXCHANGE', 'MOTIVATIONAL')),
    enabled     boolean       NOT NULL DEFAULT true,
    -- antecedências em dias (ex.: {30,7,1}); vazio nas categorias que não usam antecedência
    lead_days   integer[]     NOT NULL DEFAULT '{}',
    created_at  timestamptz   NOT NULL,
    updated_at  timestamptz   NOT NULL,
    version     bigint        NOT NULL DEFAULT 0,
    CONSTRAINT uk_notification_preference UNIQUE (user_id, category)
);

CREATE TABLE exchange_rate_alert (
    id                 uuid            PRIMARY KEY,
    user_id            uuid            NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    base               char(3)         NOT NULL REFERENCES currency (code),
    quote              char(3)         NOT NULL REFERENCES currency (code),
    condition          varchar(10)     NOT NULL CHECK (condition IN ('BELOW', 'ABOVE')),
    threshold          numeric(19, 8)  NOT NULL CHECK (threshold > 0),
    -- ARMED: pronto para disparar; TRIGGERED: já avisou e só rearma quando a cotação se afasta da meta
    state              varchar(10)     NOT NULL DEFAULT 'ARMED' CHECK (state IN ('ARMED', 'TRIGGERED')),
    last_rate          numeric(19, 8),
    last_triggered_at  timestamptz,
    trigger_count      integer         NOT NULL DEFAULT 0,
    active             boolean         NOT NULL DEFAULT true,
    created_at         timestamptz     NOT NULL,
    updated_at         timestamptz     NOT NULL,
    version            bigint          NOT NULL DEFAULT 0,
    CONSTRAINT ck_exchange_rate_alert_pair CHECK (base <> quote)
);
CREATE INDEX idx_exchange_rate_alert_active ON exchange_rate_alert (active);

-- ShedLock: um job agendado roda numa instância só
CREATE TABLE shedlock (
    name        varchar(64)   PRIMARY KEY,
    lock_until  timestamptz   NOT NULL,
    locked_at   timestamptz   NOT NULL,
    locked_by   varchar(255)  NOT NULL
);

-- A timeline deixou de mostrar pagamentos de gastos (só vencimentos): o índice da V7 ficou sem uso
DROP INDEX IF EXISTS idx_expense_payment_user_paid;
