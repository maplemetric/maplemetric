-- V8 시드는 이 직업만 클래스명 '캐논슈터'로 넣었고 나머지는 4차 전직명을 썼다.
-- Nexon 종합 랭킹은 '캐논마스터'를 돌려주므로 Alias 매칭에 실패하고, D7에 따라
-- 해당 캐릭터가 어느 직업 통계에도 잡히지 않은 채 버려진다.
--
-- Canonical 이름과 Slug는 건드리지 않는다. 표시명 변경은 별도 결정이고, 여기서는
-- Nexon 표기를 흡수하는 것만 한다 (D2).
INSERT INTO p_job_alias (job_id, alias_name, alias_type)
SELECT job_id, '캐논마스터', 'NEXON'
FROM p_job
WHERE job_name = '캐논슈터'
  AND deleted_at IS NULL;
