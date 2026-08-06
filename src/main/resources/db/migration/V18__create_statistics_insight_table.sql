-- 생성된 설명을 저장한다.
--
-- OpenAI 예산이 월 $5다. 사용자 요청마다 호출하면 트래픽이 곧 비용이 되어 상한을
-- 넘는 순간 기능이 멈춘다. 한 번 생성해 저장하면 조회 비용이 0이다.
--
-- facts_payload를 남긴다. 나중에 이 문장이 어떤 수치에서 나왔는지 추적할 수 있고,
-- Fact가 바뀌었는지 비교해 재생성 필요 여부를 판단할 수 있다.
CREATE TABLE p_statistics_insight
(
    statistics_insight_id UUID         NOT NULL DEFAULT gen_random_uuid(),
    subject_type          VARCHAR(10)  NOT NULL,
    subject_slug          VARCHAR(60)  NOT NULL,
    range_preset          VARCHAR(10)  NOT NULL,
    as_of                 DATE         NOT NULL,
    headline              VARCHAR(200) NOT NULL,
    summary               TEXT         NOT NULL,
    facts_payload         JSONB        NOT NULL,
    model                 VARCHAR(60),
    generated_at          TIMESTAMPTZ  NOT NULL,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_p_statistics_insight
        PRIMARY KEY (statistics_insight_id),

    -- 같은 대상·기간·기준일을 두 번 생성하지 않는다. 재실행이 비용을 늘리면 안 된다.
    CONSTRAINT uk_p_statistics_insight_subject_range_as_of
        UNIQUE (subject_type, subject_slug, range_preset, as_of),

    CONSTRAINT ck_p_statistics_insight_subject_type
        CHECK (subject_type IN ('JOB', 'WORLD'))
);
