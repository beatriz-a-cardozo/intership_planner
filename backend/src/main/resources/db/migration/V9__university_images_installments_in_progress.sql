-- =====================================================================
-- V9: imagens das universidades e parcelamentos já em andamento.
-- =====================================================================

-- Metadados separados do conteúdo: listar imagens nunca carrega os bytes.
CREATE TABLE university_image (
    id             uuid           PRIMARY KEY,
    user_id        uuid           NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    university_id  uuid           NOT NULL,
    content_type   varchar(20)    NOT NULL CHECK (content_type IN ('image/jpeg', 'image/png', 'image/webp')),
    size_bytes     integer        NOT NULL CHECK (size_bytes BETWEEN 1 AND 5242880),
    caption        varchar(200),
    created_at     timestamptz    NOT NULL,
    updated_at     timestamptz    NOT NULL,
    version        bigint         NOT NULL DEFAULT 0,
    CONSTRAINT fk_university_image_university FOREIGN KEY (university_id, user_id)
        REFERENCES university (id, user_id) ON DELETE CASCADE
);
CREATE INDEX idx_university_image_university ON university_image (university_id, created_at);

CREATE TABLE university_image_content (
    image_id  uuid   PRIMARY KEY REFERENCES university_image (id) ON DELETE CASCADE,
    data      bytea  NOT NULL
);

-- Capa exibida nos cards (sem consulta extra por universidade)
ALTER TABLE university ADD COLUMN cover_image_id uuid REFERENCES university_image (id) ON DELETE SET NULL;

-- "Parcela 4 de 10": as 3 primeiras foram pagas antes de entrar no app e não viram lançamentos.
-- first_due_date passa a ser o vencimento da primeira parcela acompanhada (a nº paid_before_count + 1).
ALTER TABLE installment_plan ADD COLUMN paid_before_count smallint NOT NULL DEFAULT 0;
ALTER TABLE installment_plan ADD CONSTRAINT ck_installment_plan_paid_before
    CHECK (paid_before_count >= 0 AND paid_before_count < installments_count);
