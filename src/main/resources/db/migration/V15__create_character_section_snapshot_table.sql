-- 캐릭터 조회 결과를 구간별로 저장한다.
--
-- 기존 p_character_snapshot은 (ocid, snapshot_date) 기준의 일자별 이력 모델이고
-- world_id·job_id가 NOT NULL FK라 Alias 미매칭 시 저장 자체가 실패한다. 여기서
-- 필요한 것은 "마지막으로 가져온 결과"를 덮어쓰며 보관하는 것이라 별도 테이블을 둔다.
-- 기존 두 테이블은 이력 용도로 남겨 두고 건드리지 않는다.
--
-- payload를 JSONB로 두는 이유는 캐릭터 응답 계약이 자주 늘어나기 때문이다. 최근에도
-- 익셉셔널 옵션·기본 옵션·HEXA 증가 수치·심볼 수치·스킬 설명·세트 효과가 차례로
-- 추가됐다. 이 저장본은 통째로 반환할 뿐 조건 조회를 하지 않으므로 컬럼으로 펼칠
-- 이유가 없다. 조건 조회가 필요한 집계는 랭킹 Snapshot 테이블이 담당한다.
CREATE TABLE p_character_section_snapshot
(
    character_section_snapshot_id UUID         NOT NULL DEFAULT gen_random_uuid(),
    ocid                          VARCHAR(100) NOT NULL,
    character_name                VARCHAR(30)  NOT NULL,
    section                       VARCHAR(20)  NOT NULL,
    payload                       JSONB        NOT NULL,
    fetched_at                    TIMESTAMPTZ  NOT NULL,
    created_at                    TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                    TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_p_character_section_snapshot
        PRIMARY KEY (character_section_snapshot_id),

    -- 캐릭터명은 바뀔 수 있으므로 ocid를 기준으로 덮어쓴다.
    CONSTRAINT uk_p_character_section_snapshot_ocid_section
        UNIQUE (ocid, section),

    -- 화면 탭 경계와 같은 구간이다. 탭을 열지 않으면 그 구간을 받지 않을 수 있다.
    CONSTRAINT ck_p_character_section_snapshot_section
        CHECK (section IN ('PROFILE', 'EQUIPMENT', 'SKILL'))
);

-- 캐릭터명으로 검색해 들어오므로 이름으로도 찾을 수 있어야 한다.
CREATE INDEX idx_p_character_section_snapshot_name
    ON p_character_section_snapshot (character_name, section);
