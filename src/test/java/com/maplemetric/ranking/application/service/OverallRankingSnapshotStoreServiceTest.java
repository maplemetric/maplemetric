package com.maplemetric.ranking.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.maplemetric.ranking.application.event.OverallRankingSnapshotStoredEvent;
import com.maplemetric.ranking.application.event.OverallRankingSnapshotStoredEvent.ObservedName;
import com.maplemetric.ranking.application.port.out.SaveOverallRankingSnapshotPort;
import com.maplemetric.ranking.application.port.out.SaveOverallRankingSnapshotPort.OverallRankingCollection;
import com.maplemetric.ranking.application.port.out.SaveOverallRankingSnapshotPort.RankingRow;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class OverallRankingSnapshotStoreServiceTest {

    private static final LocalDate SNAPSHOT_DATE =
            LocalDate.of(2026, 7, 27);

    @Mock
    private SaveOverallRankingSnapshotPort saveOverallRankingSnapshotPort;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Test
    void Snapshot저장후이름과행수만담은불변이벤트를발행한다() {
        OverallRankingSnapshotStoreService service = createService();
        OverallRankingCollection collection = createCollection();

        service.store(collection);

        ArgumentCaptor<OverallRankingSnapshotStoredEvent> captor =
                ArgumentCaptor.forClass(
                        OverallRankingSnapshotStoredEvent.class
                );
        InOrder inOrder = inOrder(
                saveOverallRankingSnapshotPort,
                applicationEventPublisher
        );

        inOrder.verify(saveOverallRankingSnapshotPort)
                .saveOverallRankingSnapshot(collection);
        inOrder.verify(applicationEventPublisher)
                .publishEvent(captor.capture());

        OverallRankingSnapshotStoredEvent event = captor.getValue();

        assertThat(event.snapshotDate()).isEqualTo(SNAPSHOT_DATE);
        assertThat(event.jobNames()).containsExactly(
                new ObservedName("팬텀", 1L),
                new ObservedName("  PHANTOM  ", 1L),
                new ObservedName("히어로", 1L),
                new ObservedName("비숍", 1L),
                new ObservedName("나이트로드", 1L),
                new ObservedName("아크메이지(불,독)", 1L)
        );
        assertThat(event.worldNames()).containsExactly(
                new ObservedName("루나", 1L),
                new ObservedName("  LUNA  ", 1L),
                new ObservedName("스카니아", 1L),
                new ObservedName("베라", 1L),
                new ObservedName("루나", 1L),
                new ObservedName("스카니아", 1L)
        );
        assertThatThrownBy(() -> event.jobNames().add(
                new ObservedName("히어로", 1L)
        )).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void Snapshot저장에실패하면이벤트를발행하지않는다() {
        OverallRankingSnapshotStoreService service = createService();
        OverallRankingCollection collection = createCollection();

        willThrow(new IllegalStateException("저장 실패"))
                .given(saveOverallRankingSnapshotPort)
                .saveOverallRankingSnapshot(collection);

        assertThatThrownBy(() -> service.store(collection))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("저장 실패");

        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    private OverallRankingSnapshotStoreService createService() {
        return new OverallRankingSnapshotStoreService(
                saveOverallRankingSnapshotPort,
                applicationEventPublisher
        );
    }

    private OverallRankingCollection createCollection() {
        return new OverallRankingCollection(
                SNAPSHOT_DATE,
                null,
                null,
                null,
                "NEXON_OPEN_API",
                1,
                10,
                false,
                Instant.parse("2026-07-27T00:30:00Z"),
                List.of(
                        createRow(1, "루나", "도적", "팬텀"),
                        createRow(2, "  LUNA  ", "  PHANTOM  ", null),
                        createRow(3, "스카니아", "히어로", ""),
                        createRow(4, "베라", "비숍", "   "),
                        createRow(5, "루나", "나이트로드", "\t"),
                        createRow(6, "스카니아", "아크메이지(불,독)", "\n")
                )
        );
    }

    private RankingRow createRow(
            int ranking,
            String worldName,
            String className,
            String subClassName
    ) {
        return new RankingRow(
                ranking,
                "캐릭터" + ranking,
                worldName,
                className,
                subClassName,
                300,
                0L,
                0,
                null
        );
    }
}
