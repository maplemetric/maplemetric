package com.maplemetric.character.infrastructure.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(
        name = "p_character_equipment_snapshot",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_p_character_equipment_snapshot_slot",
                        columnNames = {
                                "character_snapshot_id",
                                "equipment_preset_no",
                                "slot_name"
                        }
                )
        }
)
public class CharacterEquipmentSnapshotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            name = "character_equipment_snapshot_id",
            nullable = false,
            updatable = false
    )
    private UUID id;

    @Column(name = "character_snapshot_id", nullable = false, updatable = false)
    private UUID characterSnapshotId;

    @Column(name = "equipment_preset_no", nullable = false, updatable = false)
    private short equipmentPresetNo;

    @Column(name = "slot_name", nullable = false, updatable = false, length = 50)
    private String slotName;

    @Column(name = "item_name", nullable = false, updatable = false, length = 100)
    private String itemName;

    @Column(name = "item_icon_url", updatable = false, columnDefinition = "TEXT")
    private String itemIconUrl;

    @Column(name = "item_description", updatable = false, columnDefinition = "TEXT")
    private String itemDescription;

    @Column(name = "item_level", updatable = false)
    private Integer itemLevel;

    @Column(name = "starforce", updatable = false)
    private Integer starforce;

    @Column(name = "potential_grade", updatable = false, length = 30)
    private String potentialGrade;

    @Column(name = "additional_potential_grade", updatable = false, length = 30)
    private String additionalPotentialGrade;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "equipment_data",
            nullable = false,
            updatable = false,
            columnDefinition = "jsonb"
    )
    private JsonNode equipmentData;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected CharacterEquipmentSnapshotEntity() {
    }

    @Builder
    private CharacterEquipmentSnapshotEntity(
            UUID characterSnapshotId,
            int equipmentPresetNo,
            String slotName,
            String itemName,
            String itemIconUrl,
            String itemDescription,
            Integer itemLevel,
            Integer starforce,
            String potentialGrade,
            String additionalPotentialGrade,
            JsonNode equipmentData
    ) {
        this.characterSnapshotId = requireNonNull(
                characterSnapshotId,
                "캐릭터 Snapshot 식별자는 비어 있을 수 없습니다."
        );
        validatePresetNo(equipmentPresetNo);
        this.equipmentPresetNo = (short) equipmentPresetNo;
        this.slotName = requireText(
                slotName,
                "장비 슬롯명은 비어 있을 수 없습니다."
        );
        this.itemName = requireText(
                itemName,
                "아이템명은 비어 있을 수 없습니다."
        );
        validateNonNegative(
                itemLevel,
                "아이템 레벨은 0 이상이어야 합니다."
        );
        validateNonNegative(
                starforce,
                "스타포스는 0 이상이어야 합니다."
        );
        this.itemIconUrl = itemIconUrl;
        this.itemDescription = itemDescription;
        this.itemLevel = itemLevel;
        this.starforce = starforce;
        this.potentialGrade = potentialGrade;
        this.additionalPotentialGrade = additionalPotentialGrade;
        this.equipmentData = requireNonNull(
                equipmentData,
                "장비 원본 옵션은 비어 있을 수 없습니다."
        );
    }

    @PrePersist
    private void prePersist() {
        createdAt = Instant.now();
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

    private static <T> T requireNonNull(
            T value,
            String message
    ) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }

        return value;
    }

    private static void validatePresetNo(int equipmentPresetNo) {
        if (equipmentPresetNo < 1 || equipmentPresetNo > 3) {
            throw new IllegalArgumentException(
                    "장비 프리셋 번호는 1 이상 3 이하여야 합니다."
            );
        }
    }

    private static void validateNonNegative(
            Integer value,
            String message
    ) {
        if (value != null && value < 0) {
            throw new IllegalArgumentException(message);
        }
    }
}
