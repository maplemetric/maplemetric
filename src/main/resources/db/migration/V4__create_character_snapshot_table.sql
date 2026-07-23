CREATE TABLE p_character_snapshot
(
    character_snapshot_id UUID          NOT NULL DEFAULT gen_random_uuid(),
    ocid                  VARCHAR(100)  NOT NULL,
    character_name        VARCHAR(30)   NOT NULL,
    world_id              UUID          NOT NULL,
    job_id                UUID          NOT NULL,
    character_level       INTEGER       NOT NULL,
    character_exp         BIGINT,
    character_exp_rate    NUMERIC(7, 4),
    guild_name            VARCHAR(50),
    character_image_url   TEXT,
    gender                VARCHAR(20),
    union_level           INTEGER,
    union_grade           VARCHAR(50),
    symbols_data          JSONB,
    hexa_data             JSONB,
    union_data            JSONB,
    link_skills_data      JSONB,
    hyper_stats_data      JSONB,
    profile_data          JSONB,
    snapshot_date         DATE          NOT NULL,
    collected_at          TIMESTAMPTZ   NOT NULL,
    created_at            TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_p_character_snapshot
        PRIMARY KEY (character_snapshot_id),
    CONSTRAINT fk_p_character_snapshot_world
        FOREIGN KEY (world_id)
            REFERENCES p_world (world_id),
    CONSTRAINT fk_p_character_snapshot_job
        FOREIGN KEY (job_id)
            REFERENCES p_job (job_id),
    CONSTRAINT uk_p_character_snapshot_ocid_date
        UNIQUE (ocid, snapshot_date),
    CONSTRAINT ck_p_character_snapshot_level
        CHECK (character_level BETWEEN 1 AND 999),
    CONSTRAINT ck_p_character_snapshot_exp
        CHECK (character_exp IS NULL OR character_exp >= 0),
    CONSTRAINT ck_p_character_snapshot_exp_rate
        CHECK (
            character_exp_rate IS NULL
                OR character_exp_rate BETWEEN 0 AND 100
        ),
    CONSTRAINT ck_p_character_snapshot_union_level
        CHECK (union_level IS NULL OR union_level >= 0)
);

CREATE INDEX idx_p_character_snapshot_name_date
    ON p_character_snapshot (character_name, snapshot_date DESC);

CREATE INDEX idx_p_character_snapshot_world_job_date
    ON p_character_snapshot (world_id, job_id, snapshot_date DESC);
