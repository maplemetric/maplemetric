-- 기준일을 점유한 실행기를 가리키는 표를 둔다.
--
-- 지금까지 결과 기록은 기준일 번호와 상태만 봤다. 그러면 오래 걸려 점유가 회수된
-- 실행기의 뒤늦은 결과가, 그 사이 다시 점유한 실행기의 점유를 덮어쓴다. 같은
-- 기준일을 둘이 수집해 외부 호출을 낭비하고 시도 한도도 무너진다.
--
-- 점유할 때마다 새 표를 발급하고, 결과를 기록할 때 표가 맞아야 상태를 바꾼다.

ALTER TABLE p_overall_ranking_backfill_date
    ADD COLUMN claim_token UUID;

COMMENT ON COLUMN p_overall_ranking_backfill_date.claim_token
    IS '기준일을 점유한 실행기를 가리키는 표다. 점유 중이 아니면 비어 있다.';

-- 이 표를 쓰기 전에 점유된 채로 남은 기준일은 표가 없다. 그 실행기는 표를 모르므로
-- 결과를 기록하지 못한다. 그대로 두면 회수될 때까지 아무도 손대지 못한 채 남는다.
--
-- 그래서 여기서 점유를 미리 푼다. 결과를 알 수 없는 점유를 푸는 것은 실행기의 회수
-- 경로가 이미 하는 일이므로 그 처리와 같은 결과를 내야 한다. 회수는 재시도 가능한
-- 실패로 기록하되, 시도 횟수가 한도에 닿았으면 더 돌리지 않고 실패로 닫는다.
-- 한도를 무시하고 모두 되돌리면 이미 한도를 다 쓴 기준일이 외부를 한 번 더 호출한다.
--
-- 한도는 설정값이고 마이그레이션은 설정을 읽을 수 없다. 배포된 기본값 3을 쓴다.
-- 한도를 늘려 쓰고 있었다면 여기서 닫힌 기준일은 새 Job으로 다시 요청하면 된다.
-- 이미 수집된 기준일은 외부를 호출하지 않고 건너뛰므로 다시 요청해도 비용이 없다.
WITH closed AS (
    UPDATE p_overall_ranking_backfill_date
        SET status          = 'FAILED',
            last_error_type = 'UNKNOWN',
            finished_at     = CURRENT_TIMESTAMP,
            updated_at      = CURRENT_TIMESTAMP
        WHERE status = 'RUNNING'
            AND attempt_count >= 3
        RETURNING backfill_job_id),
     tally AS (SELECT backfill_job_id, COUNT(*) AS closed_count
               FROM closed
               GROUP BY backfill_job_id)
UPDATE p_overall_ranking_backfill_job job
SET failed_date_count = job.failed_date_count + tally.closed_count
FROM tally
WHERE job.backfill_job_id = tally.backfill_job_id;

-- 시도가 남은 기준일은 다시 잡히게 둔다. 시도 횟수와 시작 시각은 되돌리지 않는다.
-- 그 실행기가 외부를 호출했는지 여기서는 알 수 없고, 실제로 한 번 시작했던 사실은
-- 남아야 한다.
UPDATE p_overall_ranking_backfill_date
SET status      = 'PENDING',
    finished_at = NULL,
    updated_at  = CURRENT_TIMESTAMP
WHERE status = 'RUNNING';

-- 마지막 남은 기준일을 위에서 닫았다면 Job도 끝난 것이다. Job을 닫는 것은 결과를
-- 기록하는 경로가 하는 일인데, 그 경로를 더 지나지 않고 끝나 버리면 남은 기준일이
-- 없는데도 Job이 열린 채로 남는다.
UPDATE p_overall_ranking_backfill_job job
SET status      = 'FAILED',
    finished_at = CURRENT_TIMESTAMP
WHERE job.status IN ('PENDING', 'RUNNING')
  AND NOT EXISTS (SELECT 1
                  FROM p_overall_ranking_backfill_date date
                  WHERE date.backfill_job_id = job.backfill_job_id
                    AND date.status IN ('PENDING', 'RUNNING'))
  AND EXISTS (SELECT 1
              FROM p_overall_ranking_backfill_date date
              WHERE date.backfill_job_id = job.backfill_job_id
                AND date.status = 'FAILED');

-- 점유 중이 아닌 기준일에 표가 남아 있으면 표의 의미가 흐려진다.
ALTER TABLE p_overall_ranking_backfill_date
    ADD CONSTRAINT ck_p_overall_ranking_backfill_date_claim_token
        CHECK (
            (status = 'RUNNING' AND claim_token IS NOT NULL)
                OR (status <> 'RUNNING' AND claim_token IS NULL)
            );
