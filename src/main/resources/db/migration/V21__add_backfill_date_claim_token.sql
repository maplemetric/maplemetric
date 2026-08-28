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

-- 이 표를 쓰기 전에 점유된 채로 남은 기준일에는 아무도 가지지 않은 표를 발급한다.
--
-- 그 실행기들은 이 표를 모르므로 결과를 기록하지 못한다. 그것이 맞는 결과다. 이미
-- 사라진 실행기의 점유이고, 결과가 어떻게 됐는지 여기서는 알 수 없다.
--
-- 이런 점유를 정리하는 것은 실행기의 회수 경로가 이미 하는 일이다. 회수는 시작한
-- 지 오래된 점유를 찾아 재시도 가능한 실패로 기록하되, 시도 횟수가 설정된 한도에
-- 닿았으면 실패로 닫는다. 이 기준일들은 시작 시각이 이미 지났으므로 다음 실행에서
-- 곧바로 회수 대상이 된다.
--
-- 여기서 직접 PENDING으로 되돌리거나 실패로 닫지 않는다. 그 판단은 시도 한도를
-- 알아야 하는데 마이그레이션은 설정을 읽을 수 없다. 한도를 기본값으로 짐작하면,
-- 한도를 1이나 2로 줄여 쓰는 환경에서 이미 다 쓴 기준일이 외부를 한 번 더 호출한다.
UPDATE p_overall_ranking_backfill_date
SET claim_token = gen_random_uuid(),
    updated_at  = CURRENT_TIMESTAMP
WHERE status = 'RUNNING';

-- 점유 중이 아닌 기준일에 표가 남아 있으면 표의 의미가 흐려진다.
ALTER TABLE p_overall_ranking_backfill_date
    ADD CONSTRAINT ck_p_overall_ranking_backfill_date_claim_token
        CHECK (
            (status = 'RUNNING' AND claim_token IS NOT NULL)
                OR (status <> 'RUNNING' AND claim_token IS NULL)
            );
