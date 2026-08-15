-- 캐릭터명은 한 시점에 최대 한 캐릭터를 가리킨다. 저장본도 그렇게 유지한다.
--
-- V15는 ocid에만 유일 제약을 두고 캐릭터명에는 일반 인덱스만 뒀다. 캐릭터명이 바뀔 수
-- 있으니 ocid를 기준으로 삼은 것은 맞지만, 조회는 캐릭터명으로 단일 결과를 기대한다.
-- 개명으로 비게 된 이름을 다른 캐릭터가 가져가면 같은 이름의 행이 둘이 되고, 그때부터
-- 그 이름의 검색은 다중 결과 예외로 실패한다.

-- 이미 생긴 중복을 정리한다. 남기는 것은 가장 최근에 수집한 행이다.
--
-- fetched_at이 같을 수 있으므로 updated_at, 그것도 같으면 식별자까지 비교한다.
-- 기준이 결정적이지 않으면 실행 환경에 따라 다른 행이 남는다.
--
-- 저장본은 다시 수집할 수 있는 캐시이며 이력이 아니다. 지워진 행은 해당 캐릭터를
-- 다시 조회할 때 재생성된다.
DELETE
FROM p_character_section_snapshot outdated
WHERE EXISTS (SELECT 1
              FROM p_character_section_snapshot kept
              WHERE kept.character_name = outdated.character_name
                AND kept.section = outdated.section
                AND (kept.fetched_at,
                     kept.updated_at,
                     kept.character_section_snapshot_id)
                  > (outdated.fetched_at,
                     outdated.updated_at,
                     outdated.character_section_snapshot_id));

ALTER TABLE p_character_section_snapshot
    ADD CONSTRAINT uk_p_character_section_snapshot_name_section
        UNIQUE (character_name, section);

-- 유일 제약이 같은 컬럼 순서의 인덱스를 만들므로 기존 일반 인덱스는 중복이다.
DROP INDEX idx_p_character_section_snapshot_name;
