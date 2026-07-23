package com.maplemetric.character.infrastructure.persistence;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CharacterSnapshotRepository
        extends JpaRepository<CharacterSnapshotEntity, UUID> {

    Optional<CharacterSnapshotEntity> findByOcidAndSnapshotDate(
            String ocid,
            LocalDate snapshotDate
    );
}
