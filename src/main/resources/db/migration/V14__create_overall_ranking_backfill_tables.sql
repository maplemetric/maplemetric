CREATE TABLE p_overall_ranking_backfill_job
(
    backfill_job_id      UUID        NOT NULL DEFAULT gen_random_uuid(),
    requested_from       DATE        NOT NULL,
    requested_to         DATE        NOT NULL,
    status               VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    succeeded_date_count INTEGER     NOT NULL DEFAULT 0,
    failed_date_count    INTEGER     NOT NULL DEFAULT 0,
    skipped_date_count   INTEGER     NOT NULL DEFAULT 0,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at           TIMESTAMPTZ,
    finished_at          TIMESTAMPTZ,

    CONSTRAINT pk_p_overall_ranking_backfill_job
        PRIMARY KEY (backfill_job_id),
    CONSTRAINT ck_p_overall_ranking_backfill_job_range
        CHECK (requested_from <= requested_to),
    CONSTRAINT ck_p_overall_ranking_backfill_job_status
        CHECK (status IN (
            'PENDING', 'RUNNING', 'SUCCEEDED', 'FAILED', 'CANCELLED'
        )),
    CONSTRAINT ck_p_overall_ranking_backfill_job_counts
        CHECK (
            succeeded_date_count >= 0
                AND failed_date_count >= 0
                AND skipped_date_count >= 0
        )
);

CREATE TABLE p_overall_ranking_backfill_date
(
    backfill_date_id UUID        NOT NULL DEFAULT gen_random_uuid(),
    backfill_job_id  UUID        NOT NULL,
    snapshot_date    DATE        NOT NULL,
    status           VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempt_count    INTEGER     NOT NULL DEFAULT 0,
    last_error_type  VARCHAR(30),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at       TIMESTAMPTZ,
    finished_at      TIMESTAMPTZ,

    CONSTRAINT pk_p_overall_ranking_backfill_date
        PRIMARY KEY (backfill_date_id),
    CONSTRAINT fk_p_overall_ranking_backfill_date_job
        FOREIGN KEY (backfill_job_id)
            REFERENCES p_overall_ranking_backfill_job (backfill_job_id),

    -- 같은 Job에 같은 기준일 항목을 두 번 만들지 않는다.
    CONSTRAINT uk_p_overall_ranking_backfill_date_job_snapshot_date
        UNIQUE (backfill_job_id, snapshot_date),

    CONSTRAINT ck_p_overall_ranking_backfill_date_status
        CHECK (status IN (
            'PENDING', 'RUNNING', 'SUCCEEDED', 'FAILED', 'SKIPPED', 'CANCELLED'
        )),
    CONSTRAINT ck_p_overall_ranking_backfill_date_attempt_count
        CHECK (attempt_count >= 0),

    -- 오류 분류만 저장한다. 원본 응답 전문·Stack Trace·Secret은 저장하지 않는다.
    CONSTRAINT ck_p_overall_ranking_backfill_date_last_error_type
        CHECK (last_error_type IS NULL OR last_error_type IN (
            'EXTERNAL_CLIENT',
            'EXTERNAL_SERVER',
            'EXTERNAL_TIMEOUT',
            'RESPONSE_INVALID',
            'STORE_FAILED',
            'UNKNOWN'
        ))
);

-- 다음 PENDING 기준일을 Job 단위로 찾는다.
CREATE INDEX idx_p_overall_ranking_backfill_date_job_status
    ON p_overall_ranking_backfill_date (backfill_job_id, status, snapshot_date);
