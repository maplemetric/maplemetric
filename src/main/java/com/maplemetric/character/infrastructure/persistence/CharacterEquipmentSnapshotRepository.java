package com.maplemetric.character.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CharacterEquipmentSnapshotRepository
        extends JpaRepository<CharacterEquipmentSnapshotEntity, UUID> {

    List<CharacterEquipmentSnapshotEntity>
            findAllByCharacterSnapshotIdAndEquipmentPresetNoOrderBySlotNameAsc(
                    UUID characterSnapshotId,
                    short equipmentPresetNo
            );
}
