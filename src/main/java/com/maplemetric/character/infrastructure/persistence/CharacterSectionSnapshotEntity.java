package com.maplemetric.character.infrastructure.persistence;

import com.maplemetric.character.application.port.out.SaveCharacterSectionSnapshotPort.CharacterSection;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 캐릭터 조회 결과 저장본이다.
 *
 * 쓰기는 Adapter의 ON CONFLICT Upsert가 담당한다. 같은 캐릭터를 동시에 검색할 때
 * 읽고 나서 넣는 방식이 유일 제약을 위반하기 때문이다. 그래서 이 Entity에는 생성·수정
 * 경로를 두지 않고 읽기 매핑만 남긴다. {@code created_at}·{@code updated_at}은 DB
 * 기본값과 Upsert가 채운다.
 */
@Getter
@Entity
@Table(
        name = "p_character_section_snapshot",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_p_character_section_snapshot_ocid_section",
                        columnNames = {"ocid", "section"}
                )
        }
)
public class CharacterSectionSnapshotEntity {

    @Id
    @Column(
            name = "character_section_snapshot_id",
            nullable = false,
            updatable = false
    )
    private UUID id;

    @Column(name = "ocid", nullable = false, updatable = false, length = 100)
    private String ocid;

    @Column(name = "character_name", nullable = false, length = 30)
    private String characterName;

    @Enumerated(EnumType.STRING)
    @Column(name = "section", nullable = false, updatable = false, length = 20)
    private CharacterSection section;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false)
    private String payload;

    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CharacterSectionSnapshotEntity() {
    }
}
