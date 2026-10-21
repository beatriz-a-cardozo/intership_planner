-- =====================================================================
-- V4: gastos planejados, pagamentos, categorias, ciclos e ajustes manuais do objetivo
-- =====================================================================

CREATE TABLE expense_category (
    id          uuid         PRIMARY KEY,
    user_id     uuid         NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    name        varchar(60)  NOT NULL,
    color       varchar(20)  NOT NULL,
    icon        varchar(40)  NOT NULL,
    sort_order  integer      NOT NULL DEFAULT 0,
    archived    boolean      NOT NULL DEFAULT false,
    created_at  timestamptz  NOT NULL,
    updated_at  timestamptz  NOT NULL,
    version     bigint       NOT NULL DEFAULT 0,
    CONSTRAINT uk_expense_category_id_user UNIQUE (id, user_id)
);
CREATE UNIQUE INDEX uk_expense_category_name ON expense_category (user_id, lower(name));

CREATE TABLE expense_cycle (
    id          uuid         PRIMARY KEY,
    user_id     uuid         NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    name        varchar(60)  NOT NULL,
    color       varchar(20)  NOT NULL,
    icon        varchar(40)  NOT NULL,
    sort_order  integer      NOT NULL DEFAULT 0,
    archived    boolean      NOT NULL DEFAULT false,
    created_at  timestamptz  NOT NULL,
    updated_at  timestamptz  NOT NULL,
    version     bigint       NOT NULL DEFAULT 0,
    CONSTRAINT uk_expense_cycle_id_user UNIQUE (id, user_id)
);
CREATE UNIQUE INDEX uk_expense_cycle_name ON expense_cycle (user_id, lower(name));

CREATE TABLE expense (
    id            uuid            PRIMARY KEY,
    user_id       uuid            NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    objective_id  uuid            NOT NULL,
    category_id   uuid            NOT NULL,
    cycle_id      uuid,
    name          varchar(160)    NOT NULL,
    description   varchar(2000),
    -- moeda e valor ORIGINAIS (o equivalente na outra moeda é calculado na leitura)
    currency      char(3)         NOT NULL REFERENCES currency (code),
    unit_price    numeric(19, 4)  NOT NULL CHECK (unit_price >= 0),
    quantity      integer         NOT NULL DEFAULT 1 CHECK (quantity > 0),
    recurrence    varchar(12)     NOT NULL CHECK (recurrence IN ('ONE_TIME', 'MONTHLY', 'SEMIANNUAL', 'ANNUAL', 'PER_UNIT', 'CUSTOM')),
    -- sempre explícito: total = unit_price * quantity * occurrences
    occurrences   integer         NOT NULL CHECK (occurrences > 0),
    refundable    boolean         NOT NULL DEFAULT false,
    required      boolean         NOT NULL DEFAULT true,
    due_date      date,
    end_date      date,
    cancelled_at  timestamptz,
    notes         varchar(2000),
    created_at    timestamptz     NOT NULL,
    updated_at    timestamptz     NOT NULL,
    version       bigint          NOT NULL DEFAULT 0,
    CONSTRAINT uk_expense_id_user UNIQUE (id, user_id),
    CONSTRAINT fk_expense_objective FOREIGN KEY (objective_id, user_id)
        REFERENCES objective (id, user_id) ON DELETE CASCADE,
    CONSTRAINT fk_expense_category FOREIGN KEY (category_id, user_id)
        REFERENCES expense_category (id, user_id),
    CONSTRAINT fk_expense_cycle FOREIGN KEY (cycle_id, user_id)
        REFERENCES expense_cycle (id, user_id),
    CONSTRAINT ck_expense_single_occurrence CHECK (recurrence NOT IN ('ONE_TIME', 'PER_UNIT') OR occurrences = 1),
    CONSTRAINT ck_expense_dates CHECK (end_date IS NULL OR due_date IS NULL OR end_date >= due_date)
);

CREATE INDEX idx_expense_user_objective ON expense (user_id, objective_id);
CREATE INDEX idx_expense_objective_due ON expense (objective_id, due_date);
CREATE INDEX idx_expense_category ON expense (category_id);
CREATE INDEX idx_expense_cycle ON expense (cycle_id);

CREATE TABLE expense_payment (
    id                        uuid            PRIMARY KEY,
    user_id                   uuid            NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    expense_id                uuid            NOT NULL,
    paid_on                   date            NOT NULL,
    amount                    numeric(19, 4)  NOT NULL CHECK (amount > 0),
    currency                  char(3)         NOT NULL REFERENCES currency (code),
    -- taxa capturada no momento do pagamento: o histórico não muda quando a cotação muda
    rate_to_expense_currency  numeric(19, 8)  NOT NULL CHECK (rate_to_expense_currency > 0),
    note                      varchar(500),
    created_at                timestamptz     NOT NULL,
    updated_at                timestamptz     NOT NULL,
    version                   bigint          NOT NULL DEFAULT 0,
    CONSTRAINT fk_expense_payment_expense FOREIGN KEY (expense_id, user_id)
        REFERENCES expense (id, user_id) ON DELETE CASCADE
);

CREATE INDEX idx_expense_payment_expense ON expense_payment (expense_id);

-- Ajustes manuais visíveis da meta (ex.: "Sperrkonto: -75.000 BRL"), aplicados UMA vez aos totais.
CREATE TABLE objective_adjustment (
    id            uuid            PRIMARY KEY,
    user_id       uuid            NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    objective_id  uuid            NOT NULL,
    label         varchar(120)    NOT NULL,
    -- com sinal: negativo reduz a necessidade planejada
    amount        numeric(19, 4)  NOT NULL CHECK (amount <> 0),
    currency      char(3)         NOT NULL REFERENCES currency (code),
    created_at    timestamptz     NOT NULL,
    updated_at    timestamptz     NOT NULL,
    version       bigint          NOT NULL DEFAULT 0,
    CONSTRAINT fk_objective_adjustment_objective FOREIGN KEY (objective_id, user_id)
        REFERENCES objective (id, user_id) ON DELETE CASCADE
);

CREATE INDEX idx_objective_adjustment_objective ON objective_adjustment (objective_id);

-- ---------------------------------------------------------------------
-- Catálogo padrão para usuários JÁ cadastrados (novos usuários recebem o
-- mesmo catálogo no cadastro, via ExpenseCatalogSeeder — manter as listas iguais).
-- ---------------------------------------------------------------------
INSERT INTO expense_cycle (id, user_id, name, color, icon, sort_order, created_at, updated_at)
SELECT gen_random_uuid(), u.id, c.name, c.color, c.icon, c.sort_order, now(), now()
FROM app_user u
CROSS JOIN (VALUES
    ('Preparação', '#1f4d3f', 'clipboard-list', 1),
    ('Imigração',  '#2f6d9a', 'plane',          2),
    ('Vivência',   '#b7791f', 'home',           3),
    ('Emergência', '#c2533f', 'life-buoy',      4)
) AS c (name, color, icon, sort_order);

INSERT INTO expense_category (id, user_id, name, color, icon, sort_order, created_at, updated_at)
SELECT gen_random_uuid(), u.id, c.name, c.color, c.icon, c.sort_order, now(), now()
FROM app_user u
CROSS JOIN (VALUES
    ('Agência',      '#5b5f97', 'briefcase',       1),
    ('Documentos',   '#6b7280', 'file-text',       2),
    ('Proficiência', '#7c3aed', 'languages',       3),
    ('Candidatura',  '#2563eb', 'send',            4),
    ('Visto',        '#0f766e', 'stamp',           5),
    ('Universidade', '#1f4d3f', 'graduation-cap',  6),
    ('Viagem',       '#0891b2', 'plane',           7),
    ('Moradia',      '#b45309', 'home',            8),
    ('Transporte',   '#4d7c0f', 'bike',            9),
    ('Saúde',        '#dc2626', 'heart-pulse',    10),
    ('Alimentação',  '#ea580c', 'utensils',       11),
    ('Higiene',      '#0d9488', 'sparkles',       12),
    ('Pessoal',      '#db2777', 'user',           13),
    ('Emergência',   '#c2533f', 'life-buoy',      14)
) AS c (name, color, icon, sort_order);
