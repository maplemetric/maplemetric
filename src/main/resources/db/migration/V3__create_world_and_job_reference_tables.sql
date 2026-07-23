CREATE TABLE p_world
(
    world_id            UUID         NOT NULL DEFAULT gen_random_uuid(),
    world_name          VARCHAR(30)  NOT NULL,
    external_world_code VARCHAR(50),
    display_order       INTEGER      NOT NULL DEFAULT 0,
    status              VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at          TIMESTAMPTZ,

    CONSTRAINT pk_p_world
        PRIMARY KEY (world_id),
    CONSTRAINT uk_p_world_world_name
        UNIQUE (world_name),
    CONSTRAINT uk_p_world_external_world_code
        UNIQUE (external_world_code),
    CONSTRAINT ck_p_world_display_order
        CHECK (display_order >= 0),
    CONSTRAINT ck_p_world_status
        CHECK (status IN ('ACTIVE', 'CLOSED'))
);

CREATE INDEX idx_p_world_status_order
    ON p_world (status, display_order)
    WHERE deleted_at IS NULL;

CREATE TABLE p_job
(
    job_id            UUID         NOT NULL DEFAULT gen_random_uuid(),
    job_name          VARCHAR(50)  NOT NULL,
    external_job_code VARCHAR(50),
    job_group         VARCHAR(30)  NOT NULL,
    job_branch        VARCHAR(30),
    is_available      BOOLEAN      NOT NULL DEFAULT TRUE,
    display_order     INTEGER      NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at        TIMESTAMPTZ,

    CONSTRAINT pk_p_job
        PRIMARY KEY (job_id),
    CONSTRAINT uk_p_job_job_name
        UNIQUE (job_name),
    CONSTRAINT uk_p_job_external_job_code
        UNIQUE (external_job_code),
    CONSTRAINT ck_p_job_display_order
        CHECK (display_order >= 0)
);

CREATE INDEX idx_p_job_group_branch
    ON p_job (job_group, job_branch)
    WHERE deleted_at IS NULL;
