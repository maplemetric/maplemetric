ALTER TABLE p_job
    ADD COLUMN job_slug VARCHAR(80);

ALTER TABLE p_world
    ADD COLUMN world_slug VARCHAR(80);
