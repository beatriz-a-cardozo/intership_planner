-- =====================================================================
-- V7: tarefas e eventos manuais da timeline.
-- Os eventos AUTOMÁTICOS (gastos, parcelas, candidaturas, prazos, tarefas, objetivo) não são gravados:
-- são projeções calculadas na leitura a partir das tabelas de origem (ver TimelineProjector).
-- =====================================================================

CREATE TABLE task (
    id              uuid            PRIMARY KEY,
    user_id         uuid            NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    objective_id    uuid,
    title           varchar(200)    NOT NULL,
    description     varchar(2000),
    priority        varchar(10)     NOT NULL DEFAULT 'MEDIUM' CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT')),
    status          varchar(12)     NOT NULL DEFAULT 'TODO' CHECK (status IN ('TODO', 'IN_PROGRESS', 'DONE')),
    due_date        date,
    category        varchar(40),
    university_id   uuid,
    expense_id      uuid,
    application_id  uuid,
    completed_at    timestamptz,
    created_at      timestamptz     NOT NULL,
    updated_at      timestamptz     NOT NULL,
    version         bigint          NOT NULL DEFAULT 0,
    CONSTRAINT uk_task_id_user UNIQUE (id, user_id),
    CONSTRAINT ck_task_completed CHECK ((status = 'DONE') = (completed_at IS NOT NULL)),
    -- vínculos opcionais: excluir a origem só desvincula a tarefa
    CONSTRAINT fk_task_objective FOREIGN KEY (objective_id, user_id)
        REFERENCES objective (id, user_id) ON DELETE SET NULL (objective_id),
    CONSTRAINT fk_task_university FOREIGN KEY (university_id, user_id)
        REFERENCES university (id, user_id) ON DELETE SET NULL (university_id),
    CONSTRAINT fk_task_expense FOREIGN KEY (expense_id, user_id)
        REFERENCES expense (id, user_id) ON DELETE SET NULL (expense_id),
    CONSTRAINT fk_task_application FOREIGN KEY (application_id, user_id)
        REFERENCES application (id, user_id) ON DELETE SET NULL (application_id)
);
CREATE INDEX idx_task_user_status_due ON task (user_id, status, due_date);

CREATE TABLE timeline_event (
    id            uuid            PRIMARY KEY,
    user_id       uuid            NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    objective_id  uuid,
    title         varchar(200)    NOT NULL,
    description   varchar(2000),
    start_date    date            NOT NULL,
    end_date      date,
    type          varchar(16)     NOT NULL DEFAULT 'OTHER'
                  CHECK (type IN ('MILESTONE', 'TRAVEL', 'EXAM', 'DOCUMENT', 'APPOINTMENT', 'PAYMENT', 'OTHER')),
    priority      varchar(10)     NOT NULL DEFAULT 'MEDIUM' CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT')),
    done          boolean         NOT NULL DEFAULT false,
    created_at    timestamptz     NOT NULL,
    updated_at    timestamptz     NOT NULL,
    version       bigint          NOT NULL DEFAULT 0,
    CONSTRAINT ck_timeline_event_dates CHECK (end_date IS NULL OR end_date >= start_date),
    CONSTRAINT fk_timeline_event_objective FOREIGN KEY (objective_id, user_id)
        REFERENCES objective (id, user_id) ON DELETE SET NULL (objective_id)
);
CREATE INDEX idx_timeline_event_user_start ON timeline_event (user_id, start_date);

-- Índices para os projetores da timeline (uma consulta por origem, filtrada por data)
CREATE INDEX idx_expense_payment_user_paid ON expense_payment (user_id, paid_on);
-- (application_request já tem idx_application_request_user_due desde a V6)
