-- Nexon 종합 랭킹은 같은 순위를 두 행으로 돌려주는 경우가 있다.
-- 2025-12-22 1306위가 그렇다. 이름·레벨·경험치·인기도가 같고 월드만 다르다.
--
-- 기존 제약은 "한 Collection 안에서 순위는 유일하다"를 전제했는데 그 전제가 틀렸다.
-- 두 번째 행 저장이 제약에 걸려 그 기준일 전체가 영구히 수집되지 않았다.
--
-- 순위는 Nexon이 매긴 값이므로 동률을 그대로 받아들인다. 대신 같은 Collection에
-- 같은 캐릭터가 두 번 들어오는 것은 실제 오류이므로 계속 막는다. 캐릭터는
-- 이름과 월드로 식별한다.
ALTER TABLE p_overall_ranking_snapshot
    DROP CONSTRAINT uk_p_overall_ranking_snapshot_collection_rank;

ALTER TABLE p_overall_ranking_snapshot
    ADD CONSTRAINT uk_p_overall_ranking_snapshot_collection_character
        UNIQUE (overall_ranking_collection_id, character_name, world_name);

-- 순위 정렬 조회가 UNIQUE 인덱스를 쓰고 있었으므로 대체 인덱스를 만든다.
CREATE INDEX idx_p_overall_ranking_snapshot_collection_ranking
    ON p_overall_ranking_snapshot (overall_ranking_collection_id, ranking);
