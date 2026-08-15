package com.maplemetric.character.infrastructure.persistence;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maplemetric.character.application.port.out.SaveCharacterSectionSnapshotPort.CharacterSection;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

/**
 * 저장이 잠금 → 회수 → 기록 순서를 지키는지 고정한다.
 *
 * 순서가 뒤집혀 회수가 먼저 돌면 두 Transaction이 각자 회수를 마친 뒤에야 잠금을
 * 다투게 되어 잠금이 의미를 잃는다. 그 경우 먼저 기록한 쪽이 이름을 차지하고 최신
 * 수집이 버려진다.
 *
 * 동시성 테스트는 한쪽 순서를 직접 몰기 때문에 이 순서를 증명하지 못한다. 여기서는
 * 호출 순서만 본다.
 */
class CharacterSectionSnapshotSaveOrderTest {

    private static final String OCID = "test-ocid";

    private static final String CHARACTER_NAME = "감점";

    private static final Instant FETCHED_AT =
            Instant.parse("2026-08-01T00:00:00Z");

    private record TestPayload(String value) {
    }

    @Test
    void 잠금과회수를마친뒤에기록한다() {
        CharacterSectionSnapshotRepository repository =
                mock(CharacterSectionSnapshotRepository.class);

        CharacterSectionSnapshotPersistenceAdapter adapter =
                new CharacterSectionSnapshotPersistenceAdapter(
                        repository,
                        new ObjectMapper()
                );

        adapter.save(
                OCID,
                CHARACTER_NAME,
                CharacterSection.PROFILE,
                new TestPayload("value"),
                FETCHED_AT
        );

        InOrder order = inOrder(repository);

        order.verify(repository).lockCharacterName(
                CHARACTER_NAME + ":" + CharacterSection.PROFILE.name()
        );
        order.verify(repository).releaseCharacterName(
                eq(CHARACTER_NAME),
                eq(CharacterSection.PROFILE.name()),
                eq(OCID),
                eq(FETCHED_AT)
        );
        order.verify(repository).upsert(
                eq(OCID),
                eq(CHARACTER_NAME),
                eq(CharacterSection.PROFILE.name()),
                anyString(),
                eq(FETCHED_AT)
        );
        order.verifyNoMoreInteractions();
    }
}
