package com.maplemetric.character.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maplemetric.character.application.port.out.SaveCharacterSectionSnapshotPort;
import java.time.Instant;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
class CharacterSectionSnapshotPersistenceAdapter
        implements SaveCharacterSectionSnapshotPort {

    private final CharacterSectionSnapshotRepository repository;
    private final ObjectMapper objectMapper;

    CharacterSectionSnapshotPersistenceAdapter(
            CharacterSectionSnapshotRepository repository,
            ObjectMapper objectMapper
    ) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void save(
            String ocid,
            String characterName,
            CharacterSection section,
            Object payload,
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

        if (payload == null) {
            throw new IllegalArgumentException(
                    "저장할 조회 결과는 비어 있을 수 없습니다."
            );
        }

        String requiredOcid =
                requireText(ocid, "ocid는 비어 있을 수 없습니다.");
        String requiredName =
                requireText(characterName, "캐릭터명은 비어 있을 수 없습니다.");

        // 개명으로 빈 이름을 다른 캐릭터가 가져갈 수 있다. 이름은 한 저장본만
        // 가리키므로 이어받기 전에 이전 소유자의 저장본을 비운다.
        repository.releaseCharacterName(
                requiredName,
                section.name(),
                requiredOcid,
                fetchedAt
        );

        repository.upsert(
                requiredOcid,
                requiredName,
                section.name(),
                serialize(payload),
                fetchedAt
        );
    }

    @Override
    public <T> Optional<CharacterSectionSnapshot<T>> findByOcid(
            String ocid,
            CharacterSection section,
            Class<T> payloadType
    ) {
        return repository.findByOcidAndSection(ocid, section)
                .flatMap(entity -> toSnapshot(entity, payloadType));
    }

    @Override
    public <T> Optional<CharacterSectionSnapshot<T>> findByCharacterName(
            String characterName,
            CharacterSection section,
            Class<T> payloadType
    ) {
        return repository
                .findByCharacterNameAndSection(characterName, section)
                .flatMap(entity -> toSnapshot(entity, payloadType));
    }

    /**
     * 저장본을 Result로 되돌린다.
     *
     * 응답 계약이 바뀌면 예전에 저장한 형식이 더 이상 읽히지 않을 수 있다. 그때 조회
     * 전체를 실패시키면 계약 변경이 곧 장애가 되므로, 읽지 못한 저장본은 없는 것으로
     * 보고 Nexon에서 다시 가져오게 한다.
     */
    private <T> Optional<CharacterSectionSnapshot<T>> toSnapshot(
            CharacterSectionSnapshotEntity entity,
            Class<T> payloadType
    ) {
        try {
            return Optional.of(new CharacterSectionSnapshot<>(
                    entity.getOcid(),
                    entity.getCharacterName(),
                    entity.getSection(),
                    objectMapper.readValue(entity.getPayload(), payloadType),
                    entity.getFetchedAt()
            ));
        } catch (JsonProcessingException exception) {
            log.warn(
                    "캐릭터 저장본을 읽지 못해 다시 수집합니다. ocid={}, section={}",
                    entity.getOcid(),
                    entity.getSection(),
                    exception
            );

            return Optional.empty();
        }
    }

    /**
     * 조회 결과를 저장 형식으로 바꾼다.
     *
     * 직렬화 실패는 원인을 담아 던진다. 이 예외를 응답으로 노출하지 않는 것은
     * 호출자의 책임이다.
     */
    private String serialize(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException(
                    "캐릭터 조회 결과를 저장 형식으로 바꾸지 못했습니다.",
                    exception
            );
        }
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
}
