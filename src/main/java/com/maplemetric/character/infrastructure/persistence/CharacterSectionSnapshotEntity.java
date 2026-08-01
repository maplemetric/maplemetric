package com.maplemetric.character.infrastructure.persistence;

import com.maplemetric.character.application.port.out.SaveCharacterSectionSnapshotPort.CharacterSection;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
    @GeneratedValue(strategy = GenerationType.UUID)
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

    private CharacterSectionSnapshotEntity(
            String ocid,
            String characterName,
            CharacterSection section,
            String payload,
            Instant fetchedAt
    ) {
        this.ocid = requireText(ocid, "ocid는 비어 있을 수 없습니다.");
        this.characterName = requireText(
                characterName,
                "캐릭터명은 비어 있을 수 없습니다."
        );

        if (section == null) {
            throw new IllegalArgumentException(
                    "저장 구간은 비어 있을 수 없습니다."
            );
        }

        this.section = section;
        this.payload = requireText(
                payload,
                "저장할 조회 결과는 비어 있을 수 없습니다."
        );

        if (fetchedAt == null) {
            throw new IllegalArgumentException(
                    "조회 시각은 비어 있을 수 없습니다."
            );
        }

        this.fetchedAt = fetchedAt;
    }

    public static CharacterSectionSnapshotEntity create(
            String ocid,
            String characterName,
            CharacterSection section,
            String payload,
            Instant fetchedAt
    ) {
        return new CharacterSectionSnapshotEntity(
                ocid,
                characterName,
                section,
                payload,
                fetchedAt
        );
    }

    /**
     * 새로 가져온 결과로 덮어쓴다.
     *
     * 캐릭터명은 바뀔 수 있어 함께 갱신한다. ocid와 구간은 식별자라 바꾸지 않는다.
     */
    public void refresh(
            String characterName,
            String payload,
            Instant fetchedAt
    ) {
        this.characterName = requireText(
                characterName,
                "캐릭터명은 비어 있을 수 없습니다."
        );
        this.payload = requireText(
                payload,
                "저장할 조회 결과는 비어 있을 수 없습니다."
        );

        if (fetchedAt == null) {
            throw new IllegalArgumentException(
                    "조회 시각은 비어 있을 수 없습니다."
            );
        }

        this.fetchedAt = fetchedAt;
    }

    @PrePersist
    private void prePersist() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    private void preUpdate() {
        updatedAt = Instant.now();
    }

    private static String requireText(
            String value,
            String message
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }

        return value;
    }
}
