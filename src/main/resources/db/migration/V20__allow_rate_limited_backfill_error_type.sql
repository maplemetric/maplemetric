-- 한도 초과를 별도 오류 분류로 저장한다.
--
-- 지금까지 한도 초과는 EXTERNAL_CLIENT로 기록됐다. 그러면 요청 자체가 잘못된 것과
-- 구별되지 않아, 잠시 뒤면 성공할 기준일이 재시도 대상에서 빠진다.
--
-- 기존 값은 그대로 둔다. 과거 기록을 다시 해석하지 않는다.

ALTER TABLE p_overall_ranking_backfill_date
    DROP CONSTRAINT ck_p_overall_ranking_backfill_date_last_error_type;

ALTER TABLE p_overall_ranking_backfill_date
    ADD CONSTRAINT ck_p_overall_ranking_backfill_date_last_error_type
        CHECK (last_error_type IS NULL OR last_error_type IN (
            'EXTERNAL_CLIENT',
            'EXTERNAL_RATE_LIMITED',
            'EXTERNAL_SERVER',
            'EXTERNAL_TIMEOUT',
            'RESPONSE_INVALID',
            'STORE_FAILED',
            'UNKNOWN'
        ));
