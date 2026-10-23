-- =====================================================================
-- V8: malas e itens. Pesos em GRAMAS inteiros (sem arredondamento); totais e alertas são calculados
-- no backend (LuggageWeightCalculator). Item sem mala = "ainda não alocado".
-- =====================================================================

CREATE TABLE luggage_bag (
    id            uuid          PRIMARY KEY,
    user_id       uuid          NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    name          varchar(80)   NOT NULL,
    type          varchar(16)   NOT NULL DEFAULT 'CHECKED'
                  CHECK (type IN ('CARRY_ON', 'CHECKED', 'PERSONAL_ITEM', 'OTHER')),
    max_weight_g  integer       CHECK (max_weight_g IS NULL OR max_weight_g BETWEEN 1 AND 100000),
    sort_order    integer       NOT NULL DEFAULT 0,
    created_at    timestamptz   NOT NULL,
    updated_at    timestamptz   NOT NULL,
    version       bigint        NOT NULL DEFAULT 0,
    CONSTRAINT uk_luggage_bag_id_user UNIQUE (id, user_id)
);
CREATE INDEX idx_luggage_bag_user ON luggage_bag (user_id);

CREATE TABLE luggage_item (
    id             uuid           PRIMARY KEY,
    user_id        uuid           NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    bag_id         uuid,
    name           varchar(120)   NOT NULL,
    category       varchar(16)    NOT NULL DEFAULT 'OTHER'
                   CHECK (category IN ('DOCUMENTS', 'CLOTHING', 'ELECTRONICS', 'HYGIENE', 'HEALTH', 'ACCESSORIES',
                                       'FOOD', 'OTHER')),
    quantity       integer        NOT NULL DEFAULT 1 CHECK (quantity BETWEEN 1 AND 999),
    unit_weight_g  integer        CHECK (unit_weight_g IS NULL OR unit_weight_g BETWEEN 0 AND 100000),
    priority       varchar(10)    NOT NULL DEFAULT 'MEDIUM' CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT')),
    required       boolean        NOT NULL DEFAULT false,
    packed         boolean        NOT NULL DEFAULT false,
    notes          varchar(500),
    created_at     timestamptz    NOT NULL,
    updated_at     timestamptz    NOT NULL,
    version        bigint         NOT NULL DEFAULT 0,
    -- excluir a mala devolve os itens para "não alocados"
    CONSTRAINT fk_luggage_item_bag FOREIGN KEY (bag_id, user_id)
        REFERENCES luggage_bag (id, user_id) ON DELETE SET NULL (bag_id)
);
CREATE INDEX idx_luggage_item_user_bag ON luggage_item (user_id, bag_id);
