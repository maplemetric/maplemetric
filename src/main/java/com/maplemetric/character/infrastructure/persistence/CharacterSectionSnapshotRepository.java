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

    Optional<CharacterSectionSnapshotEntity> findByOcidAndSection(
            String ocid,
            CharacterSection section
    );

    Optional<CharacterSectionSnapshotEntity> findByCharacterNameAndSection(
            String characterName,
            CharacterSection section
    );
}
