DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM p_job_alias)
        OR EXISTS (SELECT 1 FROM p_world_alias) THEN
        RAISE EXCEPTION
            'p_job_alias 또는 p_world_alias에 기존 행이 있어 PRIMARY Alias Backfill을 중단합니다.';
    END IF;
END $$;

INSERT INTO p_job_alias (job_id, alias_name, alias_type)
SELECT job_id, job_name, 'PRIMARY'
FROM p_job
WHERE deleted_at IS NULL;

INSERT INTO p_world_alias (world_id, alias_name, alias_type)
SELECT world_id, world_name, 'PRIMARY'
FROM p_world
WHERE deleted_at IS NULL;
