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
-- 점유를 미리 풀어 다시 잡히게 한다. 시도 횟수는 되돌리지 않는다. 그 실행기가
-- 실제로 외부 호출을 했는지 여기서는 알 수 없어, 회수와 같은 정책을 따른다.
-- 시작 시각도 남긴다. 실제로 한 번 시작했던 사실이기 때문이다.
UPDATE p_overall_ranking_backfill_date
SET status      = 'PENDING',
    finished_at = NULL,
    updated_at  = CURRENT_TIMESTAMP
WHERE status = 'RUNNING';

-- 점유 중이 아닌 기준일에 표가 남아 있으면 표의 의미가 흐려진다.
ALTER TABLE p_overall_ranking_backfill_date
    ADD CONSTRAINT ck_p_overall_ranking_backfill_date_claim_token
        CHECK (
            (status = 'RUNNING' AND claim_token IS NOT NULL)
                OR (status <> 'RUNNING' AND claim_token IS NULL)
            );
