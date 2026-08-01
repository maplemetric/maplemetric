package com.maplemetric.character.infrastructure.persistence;

import com.maplemetric.character.application.port.out.SaveCharacterSectionSnapshotPort.CharacterSection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface CharacterSectionSnapshotRepository
        extends JpaRepository<CharacterSectionSnapshotEntity, UUID> {

    Optional<CharacterSectionSnapshotEntity> findByOcidAndSection(
            String ocid,
            CharacterSection section
    );

    Optional<CharacterSectionSnapshotEntity> findByCharacterNameAndSection(
            String characterName,
            CharacterSection section
    );
}
