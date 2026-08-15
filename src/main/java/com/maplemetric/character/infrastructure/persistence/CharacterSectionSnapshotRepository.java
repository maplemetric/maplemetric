package com.maplemetric.character.infrastructure.persistence;

import com.maplemetric.character.application.port.out.SaveCharacterSectionSnapshotPort.CharacterSection;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface CharacterSectionSnapshotRepository
        extends JpaRepository<CharacterSectionSnapshotEntity, UUID> {

    /**
     * 조회 결과를 원자적으로 덮어쓴다.
     *
     * 읽고 나서 없으면 넣는 방식은 같은 캐릭터를 동시에 검색할 때 양쪽 모두 빈 결과를
     * 보고 각자 INSERT를 시도해 유일 제약을 위반한다. 그 예외는 Transaction을
     * rollback-only로 만들어 같은 Transaction 안에서 되돌릴 수도 없다.
     * DB의 ON CONFLICT로 한 번에 처리한다.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
            value = """
                    INSERT INTO p_character_section_snapshot
                        (ocid, character_name, section, payload, fetched_at)
                    VALUES (
                        :ocid,
                        :characterName,
                        :section,
                        CAST(:payload AS JSONB),
                        :fetchedAt
                    )
                    ON CONFLICT (ocid, section)
                    DO UPDATE SET
                        character_name = EXCLUDED.character_name,
                        payload = EXCLUDED.payload,
                        fetched_at = EXCLUDED.fetched_at,
                        updated_at = now()
                    """,
            nativeQuery = true
    )
    void upsert(
            @Param("ocid") String ocid,
            @Param("characterName") String characterName,
            @Param("section") String section,
            @Param("payload") String payload,
            @Param("fetchedAt") Instant fetchedAt
    );

    /**
     * 같은 이름을 노리는 저장을 직렬화한다.
     *
     * 회수와 저장이 두 문장이라 둘 다 상대의 행을 보지 못한 채 진행할 수 있다. 그러면
     * 회수 조건의 시각 비교가 아무것도 비교하지 못한 채 먼저 넣은 쪽이 이름을 차지하고,
     * 늦게 넣은 최신 데이터가 유일 제약에 걸려 버려진다.
     *
     * 이름과 구간으로 잠가 두면 뒤 Transaction이 앞의 결과를 보고 판단한다.
     * Transaction이 끝나면 함께 풀린다.
     */
    @Query(
            value = """
                    SELECT 1
                      FROM (SELECT pg_advisory_xact_lock(
                                       CAST(hashtext(:lockKey) AS BIGINT)
                                   )) AS locked
                    """,
            nativeQuery = true
    )
    Integer lockCharacterName(@Param("lockKey") String lockKey);

    /**
     * 다른 캐릭터가 들고 있던 이름을 회수한다.
     *
     * 캐릭터명에 유일 제약이 있으므로 이름을 이어받기 전에 이전 소유자의 저장본을
     * 비워야 한다. 저장본은 다시 수집할 수 있는 캐시라 지워도 되살아난다.
     *
     * 자기보다 나중에 수집된 행은 지우지 않는다. 늦게 도착한 오래된 수집이 방금 쓰인
     * 최신 저장본을 밀어내면 안 된다. 지우지 못하면 뒤이은 저장이 유일 제약에 걸려
     * 거부되는데, 그쪽이 오래된 데이터이므로 그대로 두는 편이 맞다.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
            value = """
                    DELETE FROM p_character_section_snapshot
                     WHERE character_name = :characterName
                       AND section = :section
                       AND ocid <> :ocid
                       AND fetched_at <= :fetchedAt
                    """,
            nativeQuery = true
    )
    void releaseCharacterName(
            @Param("characterName") String characterName,
            @Param("section") String section,
            @Param("ocid") String ocid,
            @Param("fetchedAt") Instant fetchedAt
    );

    Optional<CharacterSectionSnapshotEntity> findByOcidAndSection(
            String ocid,
            CharacterSection section
    );

    Optional<CharacterSectionSnapshotEntity> findByCharacterNameAndSection(
            String characterName,
            CharacterSection section
    );
}
