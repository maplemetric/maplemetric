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
        if (section == null) {
            throw new IllegalArgumentException(
                    "저장 구간은 비어 있을 수 없습니다."
            );
        }

        if (fetchedAt == null) {
            throw new IllegalArgumentException(
                    "조회 시각은 비어 있을 수 없습니다."
            );
        }

        repository.upsert(
                requireText(ocid, "ocid는 비어 있을 수 없습니다."),
                requireText(characterName, "캐릭터명은 비어 있을 수 없습니다."),
                section.name(),
                requireText(payload, "저장할 조회 결과는 비어 있을 수 없습니다."),
                fetchedAt
        );
    }

    private String requireText(
            String value,
            String message
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }

        return value;
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
