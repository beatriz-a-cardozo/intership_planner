-- =====================================================================
-- V5: controle financeiro pessoal (substitui a aba "Economia" da planilha)
-- Lançamentos, parcelamentos, recorrências/assinaturas e metas de economia mensais.
-- Parcelas e recorrências são MATERIALIZADAS como lançamentos PLANNED (com vínculo à origem):
-- a visão mensal, os gráficos e a projeção leem uma única tabela.
-- =====================================================================

CREATE TABLE finance_category (
    id          uuid         PRIMARY KEY,
    user_id     uuid         NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    kind        varchar(10)  NOT NULL CHECK (kind IN ('INCOME', 'EXPENSE')),
    name        varchar(60)  NOT NULL,
    color       varchar(20)  NOT NULL,
    icon        varchar(40)  NOT NULL,
    sort_order  integer      NOT NULL DEFAULT 0,
    archived    boolean      NOT NULL DEFAULT false,
    created_at  timestamptz  NOT NULL,
    updated_at  timestamptz  NOT NULL,
    version     bigint       NOT NULL DEFAULT 0,
    CONSTRAINT uk_finance_category_id_user UNIQUE (id, user_id)
);
CREATE UNIQUE INDEX uk_finance_category_name ON finance_category (user_id, kind, lower(name));

CREATE TABLE installment_plan (
    id                  uuid            PRIMARY KEY,
    user_id             uuid            NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    category_id         uuid            NOT NULL,
    account_id          uuid,
    description         varchar(160)    NOT NULL,
    currency            char(3)         NOT NULL REFERENCES currency (code),
    total_amount        numeric(19, 4)  NOT NULL CHECK (total_amount > 0),
    installments_count  smallint        NOT NULL CHECK (installments_count BETWEEN 1 AND 360),
    first_due_date      date            NOT NULL,
    payment_method      varchar(20)     NOT NULL DEFAULT 'CREDIT_CARD'
                        CHECK (payment_method IN ('CREDIT_CARD', 'BOLETO', 'PIX', 'DEBIT', 'BANK_TRANSFER', 'OTHER')),
    notes               varchar(1000),
    created_at          timestamptz     NOT NULL,
    updated_at          timestamptz     NOT NULL,
    version             bigint          NOT NULL DEFAULT 0,
    CONSTRAINT uk_installment_plan_id_user UNIQUE (id, user_id),
    CONSTRAINT fk_installment_plan_category FOREIGN KEY (category_id, user_id) REFERENCES finance_category (id, user_id),
    CONSTRAINT fk_installment_plan_account FOREIGN KEY (account_id, user_id)
        REFERENCES account (id, user_id) ON DELETE SET NULL (account_id)
);
CREATE INDEX idx_installment_plan_user ON installment_plan (user_id);

CREATE TABLE recurring_entry (
    id               uuid            PRIMARY KEY,
    user_id          uuid            NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    category_id      uuid            NOT NULL,
    account_id       uuid,
    name             varchar(120)    NOT NULL,
    kind             varchar(10)     NOT NULL CHECK (kind IN ('INCOME', 'EXPENSE')),
    amount           numeric(19, 4)  NOT NULL CHECK (amount > 0),
    currency         char(3)         NOT NULL REFERENCES currency (code),
    frequency        varchar(16)     NOT NULL CHECK (frequency IN ('WEEKLY', 'MONTHLY', 'YEARLY', 'EVERY_N_MONTHS')),
    -- só para EVERY_N_MONTHS (ex.: 3 = trimestral)
    interval_months  smallint        CHECK (interval_months BETWEEN 1 AND 120),
    -- dia de cobrança (1–31; meses curtos usam o último dia). Semanal usa o dia da semana de start_date.
    billing_day      smallint        CHECK (billing_day BETWEEN 1 AND 31),
    start_date       date            NOT NULL,
    end_date         date,
    active           boolean         NOT NULL DEFAULT true,
    auto_confirm     boolean         NOT NULL DEFAULT false,
    notes            varchar(1000),
    created_at       timestamptz     NOT NULL,
    updated_at       timestamptz     NOT NULL,
    version          bigint          NOT NULL DEFAULT 0,
    CONSTRAINT uk_recurring_entry_id_user UNIQUE (id, user_id),
    CONSTRAINT fk_recurring_entry_category FOREIGN KEY (category_id, user_id) REFERENCES finance_category (id, user_id),
    CONSTRAINT fk_recurring_entry_account FOREIGN KEY (account_id, user_id)
        REFERENCES account (id, user_id) ON DELETE SET NULL (account_id),
    CONSTRAINT ck_recurring_entry_interval CHECK (frequency <> 'EVERY_N_MONTHS' OR interval_months IS NOT NULL),
    CONSTRAINT ck_recurring_entry_dates CHECK (end_date IS NULL OR end_date >= start_date)
);
CREATE INDEX idx_recurring_entry_user_active ON recurring_entry (user_id, active);

CREATE TABLE finance_transaction (
    id                  uuid            PRIMARY KEY,
    user_id             uuid            NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    account_id          uuid,
    category_id         uuid            NOT NULL,
    kind                varchar(10)     NOT NULL CHECK (kind IN ('INCOME', 'EXPENSE')),
    occurred_on         date            NOT NULL,
    amount              numeric(19, 4)  NOT NULL CHECK (amount > 0),
    currency            char(3)         NOT NULL REFERENCES currency (code),
    description         varchar(160)    NOT NULL,
    status              varchar(10)     NOT NULL DEFAULT 'CONFIRMED' CHECK (status IN ('PLANNED', 'CONFIRMED')),
    confirmed_at        timestamptz,
    -- origem: parcela (plano + número) ou ocorrência de recorrência. Apagar a origem mantém o histórico confirmado.
    installment_plan_id uuid,
    installment_number  smallint,
    recurring_entry_id  uuid,
    created_at          timestamptz     NOT NULL,
    updated_at          timestamptz     NOT NULL,
    version             bigint          NOT NULL DEFAULT 0,
    CONSTRAINT fk_finance_transaction_category FOREIGN KEY (category_id, user_id) REFERENCES finance_category (id, user_id),
    CONSTRAINT fk_finance_transaction_account FOREIGN KEY (account_id, user_id)
        REFERENCES account (id, user_id) ON DELETE SET NULL (account_id),
    CONSTRAINT fk_finance_transaction_installment FOREIGN KEY (installment_plan_id, user_id)
        REFERENCES installment_plan (id, user_id) ON DELETE SET NULL (installment_plan_id),
    CONSTRAINT fk_finance_transaction_recurring FOREIGN KEY (recurring_entry_id, user_id)
        REFERENCES recurring_entry (id, user_id) ON DELETE SET NULL (recurring_entry_id),
    CONSTRAINT ck_finance_transaction_single_source CHECK (installment_plan_id IS NULL OR recurring_entry_id IS NULL),
    CONSTRAINT uk_finance_transaction_installment UNIQUE (installment_plan_id, installment_number),
    CONSTRAINT uk_finance_transaction_recurring UNIQUE (recurring_entry_id, occurred_on)
);
CREATE INDEX idx_finance_transaction_user_date ON finance_transaction (user_id, occurred_on);
CREATE INDEX idx_finance_transaction_installment ON finance_transaction (installment_plan_id);
CREATE INDEX idx_finance_transaction_recurring ON finance_transaction (recurring_entry_id);

CREATE TABLE monthly_saving_goal (
    id          uuid            PRIMARY KEY,
    user_id     uuid            NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    -- sempre o primeiro dia do mês
    year_month  date            NOT NULL CHECK (extract(day FROM year_month) = 1),
    amount      numeric(19, 4)  NOT NULL CHECK (amount >= 0),
    currency    char(3)         NOT NULL REFERENCES currency (code),
    created_at  timestamptz     NOT NULL,
    updated_at  timestamptz     NOT NULL,
    version     bigint          NOT NULL DEFAULT 0,
    CONSTRAINT uk_monthly_saving_goal UNIQUE (user_id, year_month)
);

-- Catálogo padrão para usuários existentes (novos recebem o mesmo via FinanceCategorySeeder — manter iguais)
INSERT INTO finance_category (id, user_id, kind, name, color, icon, sort_order, created_at, updated_at)
SELECT gen_random_uuid(), u.id, c.kind, c.name, c.color, c.icon, c.sort_order, now(), now()
FROM app_user u
CROSS JOIN (VALUES
    ('INCOME',  'Salário',          '#1f4d3f', 'briefcase',      1),
    ('INCOME',  'Freelance',        '#2f6d9a', 'laptop',         2),
    ('INCOME',  'Outras receitas',  '#0f766e', 'plus-circle',    3),
    ('EXPENSE', 'Moradia',          '#b45309', 'home',           1),
    ('EXPENSE', 'Alimentação',      '#ea580c', 'utensils',       2),
    ('EXPENSE', 'Transporte',       '#4d7c0f', 'bike',           3),
    ('EXPENSE', 'Saúde',            '#dc2626', 'heart-pulse',    4),
    ('EXPENSE', 'Lazer',            '#db2777', 'sparkles',       5),
    ('EXPENSE', 'Assinaturas',      '#7c3aed', 'repeat',         6),
    ('EXPENSE', 'Educação',         '#2563eb', 'graduation-cap', 7),
    ('EXPENSE', 'Outros',           '#6b7280', 'tag',            8)
) AS c (kind, name, color, icon, sort_order);
