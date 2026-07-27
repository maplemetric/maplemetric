CREATE TABLE p_job_alias
(
    job_alias_id UUID         NOT NULL DEFAULT gen_random_uuid(),
    job_id       UUID         NOT NULL,
    alias_name   VARCHAR(50)  NOT NULL,
    alias_type   VARCHAR(20)  NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at   TIMESTAMPTZ,

    CONSTRAINT pk_p_job_alias
        PRIMARY KEY (job_alias_id),
    CONSTRAINT fk_p_job_alias_job
        FOREIGN KEY (job_id) REFERENCES p_job (job_id)
            ON DELETE RESTRICT,
    CONSTRAINT ck_p_job_alias_alias_type
        CHECK (alias_type IN ('PRIMARY', 'NEXON', 'HISTORICAL', 'MANUAL'))
);

CREATE UNIQUE INDEX uk_p_job_alias_active_name
    ON p_job_alias (LOWER(TRIM(alias_name)))
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX uk_p_job_alias_active_primary_per_job
    ON p_job_alias (job_id)
    WHERE alias_type = 'PRIMARY' AND deleted_at IS NULL;

CREATE INDEX idx_p_job_alias_job_id
    ON p_job_alias (job_id)
    WHERE deleted_at IS NULL;

CREATE TABLE p_world_alias
(
    world_alias_id UUID         NOT NULL DEFAULT gen_random_uuid(),
    world_id       UUID         NOT NULL,
    alias_name     VARCHAR(30)  NOT NULL,
    alias_type     VARCHAR(20)  NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at     TIMESTAMPTZ,

    CONSTRAINT pk_p_world_alias
        PRIMARY KEY (world_alias_id),
    CONSTRAINT fk_p_world_alias_world
        FOREIGN KEY (world_id) REFERENCES p_world (world_id)
            ON DELETE RESTRICT,
    CONSTRAINT ck_p_world_alias_alias_type
        CHECK (alias_type IN ('PRIMARY', 'NEXON', 'HISTORICAL', 'MANUAL'))
);

CREATE UNIQUE INDEX uk_p_world_alias_active_name
    ON p_world_alias (LOWER(TRIM(alias_name)))
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX uk_p_world_alias_active_primary_per_world
    ON p_world_alias (world_id)
    WHERE alias_type = 'PRIMARY' AND deleted_at IS NULL;

CREATE INDEX idx_p_world_alias_world_id
    ON p_world_alias (world_id)
    WHERE deleted_at IS NULL;
