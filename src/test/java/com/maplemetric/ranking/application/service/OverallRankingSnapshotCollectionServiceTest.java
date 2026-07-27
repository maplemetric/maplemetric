package com.maplemetric.ranking.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.maplemetric.common.nexon.NexonApiFailure;
import com.maplemetric.ranking.api.CollectOverallRankingSnapshotOutcome;
import com.maplemetric.ranking.api.CollectOverallRankingSnapshotRequest;
import com.maplemetric.ranking.api.OverallRankingCollectionAlreadyRunningException;
import com.maplemetric.ranking.api.OverallRankingCollectionException;
import com.maplemetric.ranking.api.OverallRankingCollectionFailure;
import com.maplemetric.ranking.api.OverallRankingCollectionStatus;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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

    @Mock
    private OverallRankingSnapshotStoreService overallRankingSnapshotStoreService;

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
        assertThat(result.truncated()).isFalse();

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
        assertThat(saved.requestedMaxPages()).isEqualTo(10);
        assertThat(saved.truncated()).isFalse();
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
        assertThat(result.truncated()).isTrue();

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

        verify(overallRankingSnapshotStoreService, never())
                .store(any());
    }

    @Test
    void 첫페이지기준일이요청일과다르면예외를던지고저장하지않는다() {
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
                createRanking(List.of(1), otherDate)
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

        verify(overallRankingSnapshotStoreService, never())
                .store(any());
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
        verify(overallRankingSnapshotStoreService, never())
                .store(any());
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

    @Test
    void 최대수집페이지수가상한을초과하면예외를던진다() {
        IllegalArgumentException exception =
                catchThrowableOfType(
                        () -> new CollectOverallRankingSnapshotCommand(
                                RANKING_DATE,
                                null,
                                null,
                                null,
                                101
                        ),
                        IllegalArgumentException.class
                );

        assertThat(exception).isNotNull();
    }

    @Test
    void 공개Request의월드조건을모두null로고정하여Command로변환한다() {
        OverallRankingSnapshotCollectionService service =
                createService();

        given(saveOverallRankingSnapshotPort
                .existsOverallRankingCollection(
                        RANKING_DATE, null, null, null
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
                createRanking(List.of(), RANKING_DATE)
        );

        CollectOverallRankingSnapshotOutcome outcome =
                service.collect(
                        new CollectOverallRankingSnapshotRequest(
                                RANKING_DATE, 10
                        )
                );

        assertThat(outcome.status())
                .isEqualTo(OverallRankingCollectionStatus.COLLECTED);
        assertThat(outcome.asOf()).isEqualTo(RANKING_DATE);
        assertThat(outcome.pageCount()).isEqualTo(2);
        assertThat(outcome.sampleSize()).isEqualTo(1);
        assertThat(outcome.truncated()).isFalse();

        verify(loadRankingListPort).loadOverallRanking(
                RANKING_DATE, null, null, null, 1
        );
        verify(loadRankingListPort).loadOverallRanking(
                RANKING_DATE, null, null, null, 2
        );
    }

    @Test
    void SKIPPED_Outcome은수치필드를null로변환한다() {
        OverallRankingSnapshotCollectionService service =
                createService();

        given(saveOverallRankingSnapshotPort
                .existsOverallRankingCollection(
                        RANKING_DATE, null, null, null
                ))
                .willReturn(true);

        CollectOverallRankingSnapshotOutcome outcome =
                service.collect(
                        new CollectOverallRankingSnapshotRequest(
                                RANKING_DATE, 10
                        )
                );

        assertThat(outcome.status())
                .isEqualTo(OverallRankingCollectionStatus.SKIPPED);
        assertThat(outcome.asOf()).isEqualTo(RANKING_DATE);
        assertThat(outcome.pageCount()).isNull();
        assertThat(outcome.sampleSize()).isNull();
        assertThat(outcome.truncated()).isNull();

        verifyNoInteractions(loadRankingListPort);
    }

    @Test
    void 실행중동일Collection이중복실행되면AlreadyRunning예외를던지고Nexon을호출하지않는다() {
        OverallRankingSnapshotCollectionService service =
                createService();

        CollectOverallRankingSnapshotRequest request =
                new CollectOverallRankingSnapshotRequest(
                        RANKING_DATE, 10
                );

        given(saveOverallRankingSnapshotPort
                .existsOverallRankingCollection(
                        RANKING_DATE, null, null, null
                ))
                .willAnswer(invocation -> {
                    OverallRankingCollectionAlreadyRunningException
                            concurrentException =
                            catchThrowableOfType(
                                    () -> service.collect(request),
                                    OverallRankingCollectionAlreadyRunningException.class
                            );

                    assertThat(concurrentException).isNotNull();

                    return false;
                });

        given(loadRankingListPort.loadOverallRanking(
                RANKING_DATE, null, null, null, 1
        )).willReturn(
                createRanking(List.of(), RANKING_DATE)
        );

        CollectOverallRankingSnapshotOutcome outcome =
                service.collect(request);

        assertThat(outcome.status())
                .isEqualTo(OverallRankingCollectionStatus.COLLECTED);

        verify(loadRankingListPort, times(1))
                .loadOverallRanking(
                        RANKING_DATE, null, null, null, 1
                );
    }

    @Test
    void 성공후Guard가해제되어다시수집할수있다() {
        OverallRankingSnapshotCollectionService service =
                createService();

        CollectOverallRankingSnapshotRequest request =
                new CollectOverallRankingSnapshotRequest(
                        RANKING_DATE, 10
                );

        given(saveOverallRankingSnapshotPort
                .existsOverallRankingCollection(
                        RANKING_DATE, null, null, null
                ))
                .willReturn(false);

        given(loadRankingListPort.loadOverallRanking(
                RANKING_DATE, null, null, null, 1
        )).willReturn(
                createRanking(List.of(), RANKING_DATE)
        );

        service.collect(request);

        CollectOverallRankingSnapshotOutcome second =
                service.collect(request);

        assertThat(second.status())
                .isEqualTo(OverallRankingCollectionStatus.COLLECTED);
    }

    @Test
    void Skip후Guard가해제되어다시수집할수있다() {
        OverallRankingSnapshotCollectionService service =
                createService();

        CollectOverallRankingSnapshotRequest request =
                new CollectOverallRankingSnapshotRequest(
                        RANKING_DATE, 10
                );

        given(saveOverallRankingSnapshotPort
                .existsOverallRankingCollection(
                        RANKING_DATE, null, null, null
                ))
                .willReturn(true);

        service.collect(request);

        CollectOverallRankingSnapshotOutcome second =
                service.collect(request);

        assertThat(second.status())
                .isEqualTo(OverallRankingCollectionStatus.SKIPPED);
    }

    @Test
    void 예외후Guard가해제되어다시수집할수있다() {
        OverallRankingSnapshotCollectionService service =
                createService();

        CollectOverallRankingSnapshotRequest request =
                new CollectOverallRankingSnapshotRequest(
                        RANKING_DATE, 10
                );

        LocalDate otherDate = RANKING_DATE.plusDays(1);

        given(saveOverallRankingSnapshotPort
                .existsOverallRankingCollection(
                        RANKING_DATE, null, null, null
                ))
                .willReturn(false);

        given(loadRankingListPort.loadOverallRanking(
                RANKING_DATE, null, null, null, 1
        )).willReturn(
                createRanking(List.of(1), otherDate)
        );

        catchThrowableOfType(
                () -> service.collect(request),
                OverallRankingCollectionException.class
        );

        given(loadRankingListPort.loadOverallRanking(
                RANKING_DATE, null, null, null, 1
        )).willReturn(
                createRanking(List.of(), RANKING_DATE)
        );

        CollectOverallRankingSnapshotOutcome second =
                service.collect(request);

        assertThat(second.status())
                .isEqualTo(OverallRankingCollectionStatus.COLLECTED);
    }

    @ParameterizedTest
    @CsvSource({
            "NOT_FOUND, EXTERNAL_API_CLIENT_ERROR",
            "CLIENT_ERROR, EXTERNAL_API_CLIENT_ERROR",
            "SERVER_ERROR, EXTERNAL_API_SERVER_ERROR",
            "TIMEOUT, EXTERNAL_API_TIMEOUT",
            "RESPONSE_INVALID, EXTERNAL_API_RESPONSE_INVALID"
    })
    void Nexon실패는공개Collection실패로변환된다(
            NexonApiFailure nexonApiFailure,
            OverallRankingCollectionFailure expected
    ) {
        OverallRankingSnapshotCollectionService service =
                createService();

        given(saveOverallRankingSnapshotPort
                .existsOverallRankingCollection(
                        RANKING_DATE, null, null, null
                ))
                .willReturn(false);

        given(loadRankingListPort.loadOverallRanking(
                RANKING_DATE, null, null, null, 1
        )).willThrow(new RankingException(nexonApiFailure));

        OverallRankingCollectionException exception =
                catchThrowableOfType(
                        () -> service.collect(
                                new CollectOverallRankingSnapshotRequest(
                                        RANKING_DATE, 10
                                )
                        ),
                        OverallRankingCollectionException.class
                );

        assertThat(exception).isNotNull();
        assertThat(exception.getFailure()).isEqualTo(expected);

        verify(overallRankingSnapshotStoreService, never())
                .store(any());
    }

    @Test
    void 내부RankingException은공개계약밖으로전파되지않는다() {
        OverallRankingSnapshotCollectionService service =
                createService();

        given(saveOverallRankingSnapshotPort
                .existsOverallRankingCollection(
                        RANKING_DATE, null, null, null
                ))
                .willReturn(false);

        given(loadRankingListPort.loadOverallRanking(
                RANKING_DATE, null, null, null, 1
        )).willThrow(
                new RankingException(NexonApiFailure.SERVER_ERROR)
        );

        assertThatThrownBy(() -> service.collect(
                new CollectOverallRankingSnapshotRequest(RANKING_DATE, 10)
        ))
                .isInstanceOf(OverallRankingCollectionException.class)
                .isNotInstanceOf(RankingException.class);
    }

    @Test
    void 예상하지못한예외는그대로전파된다() {
        OverallRankingSnapshotCollectionService service =
                createService();

        given(saveOverallRankingSnapshotPort
                .existsOverallRankingCollection(
                        RANKING_DATE, null, null, null
                ))
                .willThrow(new IllegalStateException("boom"));

        assertThatThrownBy(() -> service.collect(
                new CollectOverallRankingSnapshotRequest(RANKING_DATE, 10)
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("boom");
    }

    private OverallRankingCollection captureSavedCollection() {
        ArgumentCaptor<OverallRankingCollection> captor =
                ArgumentCaptor.forClass(
                        OverallRankingCollection.class
                );

        verify(overallRankingSnapshotStoreService, times(1))
                .store(captor.capture());

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
                overallRankingSnapshotStoreService,
                Clock.fixed(
                        Instant.parse("2026-07-21T00:30:00Z"),
                        ZoneId.of("Asia/Seoul")
                )
        );
    }
}
