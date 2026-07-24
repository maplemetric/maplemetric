package com.maplemetric.ranking.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.maplemetric.ranking.application.command.CollectOverallRankingSnapshotCommand;
import com.maplemetric.ranking.application.port.out.LoadRankingListPort;
import com.maplemetric.ranking.application.port.out.SaveOverallRankingSnapshotPort;
import com.maplemetric.ranking.application.port.out.SaveOverallRankingSnapshotPort.OverallRankingCollection;
import com.maplemetric.ranking.application.result.CollectOverallRankingSnapshotResult;
import com.maplemetric.ranking.application.result.GetOverallRankingResult;
import com.maplemetric.ranking.domain.exception.RankingException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OverallRankingSnapshotCollectionServiceTest {

    private static final LocalDate RANKING_DATE =
            LocalDate.of(2026, 7, 19);

    @Mock
    private LoadRankingListPort loadRankingListPort;

    @Mock
    private SaveOverallRankingSnapshotPort saveOverallRankingSnapshotPort;

    @Test
    void 빈페이지를만나면수집을종료하고저장한다() {
        OverallRankingSnapshotCollectionService service =
                createService();

        given(saveOverallRankingSnapshotPort
                .existsOverallRankingCollection(
                        RANKING_DATE,
                        null,
                        null,
                        null
                ))
                .willReturn(false);

        given(loadRankingListPort.loadOverallRanking(
                RANKING_DATE,
                null,
                null,
                null,
                1
        )).willReturn(
                createRanking(
                        List.of(1, 2),
                        RANKING_DATE
                )
        );

        given(loadRankingListPort.loadOverallRanking(
                RANKING_DATE,
                null,
                null,
                null,
                2
        )).willReturn(
                createRanking(
                        List.of(),
                        RANKING_DATE
                )
        );

        CollectOverallRankingSnapshotResult result =
                service.collectOverallRanking(
                        new CollectOverallRankingSnapshotCommand(
                                RANKING_DATE,
                                null,
                                null,
                                null,
                                10
                        )
                );

        assertThat(result.collected()).isTrue();
        assertThat(result.asOf()).isEqualTo(RANKING_DATE);
        assertThat(result.pageCount()).isEqualTo(2);
        assertThat(result.sampleSize()).isEqualTo(2);

        verify(loadRankingListPort).loadOverallRanking(
                RANKING_DATE, null, null, null, 1
        );
        verify(loadRankingListPort).loadOverallRanking(
                RANKING_DATE, null, null, null, 2
        );
        verify(loadRankingListPort, never()).loadOverallRanking(
                RANKING_DATE, null, null, null, 3
        );

        OverallRankingCollection saved =
                captureSavedCollection();

        assertThat(saved.pageCount()).isEqualTo(2);
        assertThat(saved.rows()).hasSize(2);
        assertThat(saved.rows())
                .extracting(row -> row.ranking())
                .containsExactly(1, 2);
    }

    @Test
    void maxPages상한에도달하면절단수집한다() {
        OverallRankingSnapshotCollectionService service =
                createService();

        given(saveOverallRankingSnapshotPort
                .existsOverallRankingCollection(
                        RANKING_DATE,
                        "루나",
                        0,
                        "팬텀"
                ))
                .willReturn(false);

        given(loadRankingListPort.loadOverallRanking(
                RANKING_DATE, "루나", 0, "팬텀", 1
        )).willReturn(
                createRanking(List.of(1, 2), RANKING_DATE)
        );

        given(loadRankingListPort.loadOverallRanking(
                RANKING_DATE, "루나", 0, "팬텀", 2
        )).willReturn(
                createRanking(List.of(3, 4), RANKING_DATE)
        );

        CollectOverallRankingSnapshotResult result =
                service.collectOverallRanking(
                        new CollectOverallRankingSnapshotCommand(
                                RANKING_DATE,
                                "루나",
                                0,
                                "팬텀",
                                2
                        )
                );

        assertThat(result.pageCount()).isEqualTo(2);
        assertThat(result.sampleSize()).isEqualTo(4);

        verify(loadRankingListPort, never()).loadOverallRanking(
                RANKING_DATE, "루나", 0, "팬텀", 3
        );
    }

    @Test
    void 페이지간기준일이다르면예외를던지고저장하지않는다() {
        OverallRankingSnapshotCollectionService service =
                createService();

        LocalDate otherDate =
                RANKING_DATE.plusDays(1);

        given(saveOverallRankingSnapshotPort
                .existsOverallRankingCollection(
                        RANKING_DATE,
                        null,
                        null,
                        null
                ))
                .willReturn(false);

        given(loadRankingListPort.loadOverallRanking(
                RANKING_DATE, null, null, null, 1
        )).willReturn(
                createRanking(List.of(1), RANKING_DATE)
        );

        given(loadRankingListPort.loadOverallRanking(
                RANKING_DATE, null, null, null, 2
        )).willReturn(
                createRanking(List.of(2), otherDate)
        );

        RankingException exception =
                catchThrowableOfType(
                        () -> service.collectOverallRanking(
                                new CollectOverallRankingSnapshotCommand(
                                        RANKING_DATE,
                                        null,
                                        null,
                                        null,
                                        10
                                )
                        ),
                        RankingException.class
                );

        assertThat(exception).isNotNull();

        verify(saveOverallRankingSnapshotPort, never())
                .saveOverallRankingSnapshot(any());
    }

    @Test
    void 이미수집된조건이면Nexon을호출하지않고건너뛴다() {
        OverallRankingSnapshotCollectionService service =
                createService();

        given(saveOverallRankingSnapshotPort
                .existsOverallRankingCollection(
                        RANKING_DATE,
                        null,
                        null,
                        null
                ))
                .willReturn(true);

        CollectOverallRankingSnapshotResult result =
                service.collectOverallRanking(
                        new CollectOverallRankingSnapshotCommand(
                                RANKING_DATE,
                                null,
                                null,
                                null,
                                10
                        )
                );

        assertThat(result.collected()).isFalse();
        assertThat(result.asOf()).isEqualTo(RANKING_DATE);

        verifyNoInteractions(loadRankingListPort);
        verify(saveOverallRankingSnapshotPort, never())
                .saveOverallRankingSnapshot(any());
    }

    @Test
    void 최대수집페이지수가0이하이면예외를던진다() {
        IllegalArgumentException exception =
                catchThrowableOfType(
                        () -> new CollectOverallRankingSnapshotCommand(
                                RANKING_DATE,
                                null,
                                null,
                                null,
                                0
                        ),
                        IllegalArgumentException.class
                );

        assertThat(exception).isNotNull();
    }

    private OverallRankingCollection captureSavedCollection() {
        ArgumentCaptor<OverallRankingCollection> captor =
                ArgumentCaptor.forClass(
                        OverallRankingCollection.class
                );

        verify(saveOverallRankingSnapshotPort, times(1))
                .saveOverallRankingSnapshot(captor.capture());

        return captor.getValue();
    }

    private GetOverallRankingResult createRanking(
            List<Integer> ranks,
            LocalDate asOf
    ) {
        List<GetOverallRankingResult.Ranking> ranking =
                ranks.stream()
                        .map(rank -> new GetOverallRankingResult.Ranking(
                                rank,
                                "캐릭터" + rank,
                                "루나",
                                "팬텀",
                                null,
                                200,
                                0L,
                                0,
                                null
                        ))
                        .toList();

        return GetOverallRankingResult.of(
                ranking,
                1,
                asOf,
                "NEXON_OPEN_API"
        );
    }

    private OverallRankingSnapshotCollectionService createService() {
        return new OverallRankingSnapshotCollectionService(
                loadRankingListPort,
                saveOverallRankingSnapshotPort,
                Clock.fixed(
                        Instant.parse("2026-07-21T00:30:00Z"),
                        ZoneId.of("Asia/Seoul")
                )
        );
    }
}
