DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM p_job WHERE job_slug IS NULL)
        OR EXISTS (SELECT 1 FROM p_world WHERE world_slug IS NULL) THEN
        RAISE EXCEPTION
            'NULL Slug가 있어 NOT NULL 제약 적용을 중단합니다.';
    END IF;

    IF EXISTS (
        SELECT job_slug
        FROM p_job
        GROUP BY job_slug
        HAVING COUNT(*) > 1
    ) OR EXISTS (
        SELECT world_slug
        FROM p_world
        GROUP BY world_slug
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION
            '중복 Slug가 있어 UNIQUE 제약 적용을 중단합니다.';
    END IF;
END $$;

ALTER TABLE p_job
    ADD CONSTRAINT uk_p_job_job_slug
        UNIQUE (job_slug);

ALTER TABLE p_world
    ADD CONSTRAINT uk_p_world_world_slug
        UNIQUE (world_slug);

ALTER TABLE p_job
    ALTER COLUMN job_slug SET NOT NULL;

ALTER TABLE p_world
    ALTER COLUMN world_slug SET NOT NULL;
