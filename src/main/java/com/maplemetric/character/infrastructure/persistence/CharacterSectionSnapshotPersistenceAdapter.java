package com.maplemetric.character.infrastructure.persistence;

import com.maplemetric.character.application.port.out.SaveCharacterSectionSnapshotPort;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
class CharacterSectionSnapshotPersistenceAdapter
        implements SaveCharacterSectionSnapshotPort {

    private final CharacterSectionSnapshotRepository repository;

    CharacterSectionSnapshotPersistenceAdapter(
            CharacterSectionSnapshotRepository repository
    ) {
        this.repository = repository;
    }

    @Override
    public void save(
            String ocid,
            String characterName,
            CharacterSection section,
            String payload,
            Instant fetchedAt
    ) {
        Optional<CharacterSectionSnapshotEntity> stored =
                repository.findByOcidAndSection(ocid, section);

        if (stored.isPresent()) {
            stored.get().refresh(characterName, payload, fetchedAt);
            return;
        }

        repository.save(CharacterSectionSnapshotEntity.create(
                ocid,
                characterName,
                section,
                payload,
                fetchedAt
        ));
    }

    @Override
    public Optional<CharacterSectionSnapshot> findByOcid(
            String ocid,
            CharacterSection section
    ) {
        return repository.findByOcidAndSection(ocid, section)
                .map(entity -> toSnapshot(entity));
    }

    @Override
    public Optional<CharacterSectionSnapshot> findByCharacterName(
            String characterName,
            CharacterSection section
    ) {
        return repository
                .findByCharacterNameAndSection(characterName, section)
                .map(entity -> toSnapshot(entity));
    }

    private CharacterSectionSnapshot toSnapshot(
            CharacterSectionSnapshotEntity entity
    ) {
        return new CharacterSectionSnapshot(
                entity.getOcid(),
                entity.getCharacterName(),
                entity.getSection(),
                entity.getPayload(),
                entity.getFetchedAt()
        );
    }
}
