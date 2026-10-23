-- =====================================================================
-- V11: sessões de importação da planilha. A prévia (linhas lidas, avisos e rascunhos) fica guardada até o
-- "importar"; o commit lê daqui e não precisa do arquivo de novo. Uma sessão só pode ser importada uma vez.
-- =====================================================================

CREATE TABLE import_session (
    id            uuid           PRIMARY KEY,
    user_id       uuid           NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    file_name     varchar(200)   NOT NULL,
    status        varchar(12)    NOT NULL DEFAULT 'PREVIEW' CHECK (status IN ('PREVIEW', 'COMMITTED')),
    payload       jsonb          NOT NULL,
    summary       jsonb,
    expires_at    timestamptz    NOT NULL,
    committed_at  timestamptz,
    created_at    timestamptz    NOT NULL,
    updated_at    timestamptz    NOT NULL,
    version       bigint         NOT NULL DEFAULT 0
);
CREATE INDEX idx_import_session_user ON import_session (user_id, created_at DESC);
