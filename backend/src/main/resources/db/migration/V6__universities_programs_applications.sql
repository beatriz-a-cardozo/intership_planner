-- =====================================================================
-- V6: universidades, programas (com requisitos de idioma e intakes), candidaturas,
-- histórico de status e solicitações da universidade.
-- Universidades e programas pertencem ao usuário (sem catálogo global por enquanto).
-- =====================================================================

CREATE TABLE university (
    id                       uuid            PRIMARY KEY,
    user_id                  uuid            NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    name                     varchar(200)    NOT NULL,
    ranking                  integer         CHECK (ranking > 0),
    institution_type         varchar(10)     CHECK (institution_type IN ('PUBLIC', 'PRIVATE')),
    nature                   varchar(12)     CHECK (nature IN ('UNIVERSITY', 'TU', 'HOCHSCHULE', 'OTHER')),
    city                     varchar(120),
    region                   varchar(120),
    country_code             char(2),
    population               integer         CHECK (population >= 0),
    -- preparado para o mapa futuro; não importado da planilha
    latitude                 numeric(9, 6)   CHECK (latitude BETWEEN -90 AND 90),
    longitude                numeric(9, 6)   CHECK (longitude BETWEEN -180 AND 180),
    cost_of_living_amount    numeric(19, 4)  CHECK (cost_of_living_amount >= 0),
    cost_of_living_currency  char(3)         REFERENCES currency (code),
    employability            varchar(12)     CHECK (employability IN ('LOW', 'LOW_MEDIUM', 'MEDIUM', 'MEDIUM_HIGH', 'HIGH', 'EXCELLENT')),
    website                  varchar(500),
    notes                    varchar(2000),
    created_at               timestamptz     NOT NULL,
    updated_at               timestamptz     NOT NULL,
    version                  bigint          NOT NULL DEFAULT 0,
    CONSTRAINT uk_university_id_user UNIQUE (id, user_id),
    CONSTRAINT ck_university_cost_currency CHECK ((cost_of_living_amount IS NULL) = (cost_of_living_currency IS NULL))
);
CREATE INDEX idx_university_user_country ON university (user_id, country_code);

CREATE TABLE program (
    id                          uuid            PRIMARY KEY,
    user_id                     uuid            NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    university_id               uuid            NOT NULL,
    name                        varchar(200)    NOT NULL,
    area                        varchar(500),
    description                 varchar(2000),
    official_url                varchar(500),
    degree                      varchar(40),
    language_summary            varchar(60),
    duration_semesters          smallint        CHECK (duration_semesters BETWEEN 1 AND 20),
    gre                         varchar(13)     NOT NULL DEFAULT 'UNKNOWN',
    aptitude_assessment         varchar(13)     NOT NULL DEFAULT 'UNKNOWN',
    interview                   varchar(13)     NOT NULL DEFAULT 'UNKNOWN',
    nc                          varchar(13)     NOT NULL DEFAULT 'UNKNOWN',
    application_channel         varchar(12)     CHECK (application_channel IN ('DIRECT', 'UNI_ASSIST', 'VPD', 'PORTAL')),
    portal_name                 varchar(60),
    document_submission         varchar(10)     CHECK (document_submission IN ('ONLINE', 'PHYSICAL', 'BOTH')),
    semester_fee_amount         numeric(19, 4)  CHECK (semester_fee_amount >= 0),
    semester_fee_currency       char(3)         REFERENCES currency (code),
    non_eu_tuition_amount       numeric(19, 4)  CHECK (non_eu_tuition_amount >= 0),
    non_eu_tuition_currency     char(3)         REFERENCES currency (code),
    non_eu_tuition_period       varchar(10)     CHECK (non_eu_tuition_period IN ('SEMESTER', 'YEAR', 'TOTAL')),
    scholarship_available       boolean,
    scholarship_details         varchar(500),
    scholarship_chance          varchar(12)     CHECK (scholarship_chance IN ('LOW', 'LOW_MEDIUM', 'MEDIUM', 'MEDIUM_HIGH', 'HIGH')),
    admission_chance            varchar(12)     CHECK (admission_chance IN ('LOW', 'LOW_MEDIUM', 'MEDIUM', 'MEDIUM_HIGH', 'HIGH')),
    -- texto original do prazo (ex.: "01/Dezembro - 01/Março"); a janela estruturada fica em program_intake
    application_deadline_text   varchar(200),
    chosen                      boolean         NOT NULL DEFAULT false,
    notes                       varchar(2000),
    created_at                  timestamptz     NOT NULL,
    updated_at                  timestamptz     NOT NULL,
    version                     bigint          NOT NULL DEFAULT 0,
    CONSTRAINT uk_program_id_user UNIQUE (id, user_id),
    CONSTRAINT fk_program_university FOREIGN KEY (university_id, user_id) REFERENCES university (id, user_id) ON DELETE CASCADE,
    CONSTRAINT ck_program_requirements CHECK (
        gre IN ('REQUIRED', 'NOT_REQUIRED', 'CONDITIONAL', 'UNKNOWN')
        AND aptitude_assessment IN ('REQUIRED', 'NOT_REQUIRED', 'CONDITIONAL', 'UNKNOWN')
        AND interview IN ('REQUIRED', 'NOT_REQUIRED', 'CONDITIONAL', 'UNKNOWN')
        AND nc IN ('REQUIRED', 'NOT_REQUIRED', 'CONDITIONAL', 'UNKNOWN')),
    CONSTRAINT ck_program_fee_currency CHECK ((semester_fee_amount IS NULL) = (semester_fee_currency IS NULL)),
    CONSTRAINT ck_program_tuition_currency CHECK ((non_eu_tuition_amount IS NULL) = (non_eu_tuition_currency IS NULL))
);
CREATE INDEX idx_program_university ON program (university_id);
CREATE INDEX idx_program_user ON program (user_id);

CREATE TABLE program_language_requirement (
    id          uuid           PRIMARY KEY,
    program_id  uuid           NOT NULL REFERENCES program (id) ON DELETE CASCADE,
    -- ISO 639-1 (en, de...)
    language    varchar(5)     NOT NULL,
    min_cefr    varchar(2)     CHECK (min_cefr IN ('A1', 'A2', 'B1', 'B2', 'C1', 'C2')),
    test_name   varchar(30),
    min_score   numeric(6, 1)  CHECK (min_score >= 0),
    raw_text    varchar(200),
    sort_order  smallint       NOT NULL DEFAULT 0
);
CREATE INDEX idx_program_language_requirement_program ON program_language_requirement (program_id);

-- Janela de inscrição RECORRENTE (dia/mês); a candidatura tem as datas concretas de um ciclo.
CREATE TABLE program_intake (
    id           uuid         PRIMARY KEY,
    program_id   uuid         NOT NULL REFERENCES program (id) ON DELETE CASCADE,
    term         varchar(10)  NOT NULL CHECK (term IN ('WINTER', 'SUMMER', 'OTHER')),
    open_month   smallint     CHECK (open_month BETWEEN 1 AND 12),
    open_day     smallint     CHECK (open_day BETWEEN 1 AND 31),
    close_month  smallint     CHECK (close_month BETWEEN 1 AND 12),
    close_day    smallint     CHECK (close_day BETWEEN 1 AND 31),
    -- mês de início das aulas (inverno = 10, verão = 4)
    start_month  smallint     NOT NULL CHECK (start_month BETWEEN 1 AND 12),
    CONSTRAINT ck_program_intake_open CHECK ((open_month IS NULL) = (open_day IS NULL)),
    CONSTRAINT ck_program_intake_close CHECK ((close_month IS NULL) = (close_day IS NULL))
);
CREATE INDEX idx_program_intake_program ON program_intake (program_id);

CREATE TABLE application (
    id                       uuid          PRIMARY KEY,
    user_id                  uuid          NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    objective_id             uuid,
    program_id               uuid          NOT NULL,
    status                   varchar(30)   NOT NULL DEFAULT 'AWAITING_OPENING' CHECK (status IN (
                                 'AWAITING_OPENING', 'DOCUMENTS_UNDER_REVIEW', 'EXAM_REQUESTED', 'INTERVIEW_REQUESTED',
                                 'ITEM_REQUESTED', 'WAITLISTED', 'AWAITING_RESULT', 'ACCEPTED', 'REJECTED', 'ENROLLED')),
    intake_year              smallint      NOT NULL CHECK (intake_year BETWEEN 2000 AND 2100),
    term                     varchar(10)   NOT NULL CHECK (term IN ('WINTER', 'SUMMER', 'OTHER')),
    archived_at              timestamptz,
    submitted_at             date,
    deadline                 date,
    response_min_date        date,
    response_likely_date     date,
    response_max_date        date,
    response_confidence      varchar(6)    CHECK (response_confidence IN ('LOW', 'MEDIUM', 'HIGH')),
    response_estimate_basis  varchar(500),
    actual_response_date     date,
    notes                    varchar(2000),
    created_at               timestamptz   NOT NULL,
    updated_at               timestamptz   NOT NULL,
    version                  bigint        NOT NULL DEFAULT 0,
    CONSTRAINT uk_application_id_user UNIQUE (id, user_id),
    CONSTRAINT uk_application_program_cycle UNIQUE (user_id, program_id, intake_year, term),
    CONSTRAINT fk_application_program FOREIGN KEY (program_id, user_id) REFERENCES program (id, user_id) ON DELETE CASCADE,
    CONSTRAINT fk_application_objective FOREIGN KEY (objective_id, user_id)
        REFERENCES objective (id, user_id) ON DELETE SET NULL (objective_id),
    CONSTRAINT ck_application_response_order CHECK (
        (response_min_date IS NULL OR response_likely_date IS NULL OR response_min_date <= response_likely_date)
        AND (response_likely_date IS NULL OR response_max_date IS NULL OR response_likely_date <= response_max_date)
        AND (response_min_date IS NULL OR response_max_date IS NULL OR response_min_date <= response_max_date))
);
CREATE INDEX idx_application_user_status ON application (user_id, status);
CREATE INDEX idx_application_deadline ON application (deadline);
CREATE INDEX idx_application_response_likely ON application (response_likely_date);

CREATE TABLE application_status_history (
    id              uuid          PRIMARY KEY,
    application_id  uuid          NOT NULL REFERENCES application (id) ON DELETE CASCADE,
    status          varchar(30)   NOT NULL,
    changed_on      date          NOT NULL,
    note            varchar(500),
    created_at      timestamptz   NOT NULL
);
CREATE INDEX idx_application_status_history_application ON application_status_history (application_id);

CREATE TABLE application_request (
    id              uuid          PRIMARY KEY,
    user_id         uuid          NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    application_id  uuid          NOT NULL,
    type            varchar(16)   NOT NULL CHECK (type IN ('DOCUMENT', 'EXAM', 'INTERVIEW', 'LETTER', 'RECOMMENDATION', 'TEST', 'PAYMENT', 'OTHER')),
    description     varchar(500)  NOT NULL,
    requested_on    date          NOT NULL,
    due_date        date,
    fulfilled_on    date,
    created_at      timestamptz   NOT NULL,
    updated_at      timestamptz   NOT NULL,
    version         bigint        NOT NULL DEFAULT 0,
    CONSTRAINT fk_application_request_application FOREIGN KEY (application_id, user_id)
        REFERENCES application (id, user_id) ON DELETE CASCADE
);
CREATE INDEX idx_application_request_application ON application_request (application_id);
CREATE INDEX idx_application_request_user_due ON application_request (user_id, due_date);
