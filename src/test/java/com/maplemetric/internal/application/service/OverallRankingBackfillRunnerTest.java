package com.maplemetric.internal.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.maplemetric.internal.application.port.out.OverallRankingBackfillStatePort.BackfillDate;
import com.maplemetric.internal.application.port.out.OverallRankingBackfillStatePort.BackfillErrorType;
import com.maplemetric.internal.application.port.out.OverallRankingBackfillStatePort.BackfillStatus;
import com.maplemetric.internal.application.properties.OverallRankingBackfillProperties;
import com.maplemetric.ranking.api.CollectOverallRankingSnapshotOutcome;
import com.maplemetric.ranking.api.CollectOverallRankingSnapshotRequest;
import com.maplemetric.ranking.api.CollectOverallRankingSnapshotUseCase;
import com.maplemetric.ranking.api.OverallRankingCollectionAlreadyRunningException;
import com.maplemetric.ranking.api.OverallRankingCollectionException;
import com.maplemetric.ranking.api.OverallRankingCollectionFailure;
import com.maplemetric.ranking.api.OverallRankingCollectionStatus;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OverallRankingBackfillRunnerTest {

    private static final UUID JOB_ID = UUID.randomUUID();

    private static final LocalDate FIRST_DATE = LocalDate.of(2026, 7, 1);

    private static final int MAX_PAGES = 10;

    private static final int MAX_ATTEMPTS = 3;

    private static final Duration STALE_CLAIM_TIMEOUT =
            Duration.ofMinutes(10);

    @Mock
    private OverallRankingBackfillStateService backfillStateService;

    @Mock
    private CollectOverallRankingSnapshotUseCase collectUseCase;

    private RecordingSleeper sleeper;

    private OverallRankingBackfillRunner runner;

    @BeforeEach
    void setUp() {
        sleeper = new RecordingSleeper();
        runner = createRunner(7);
    }

    @Test
    void 남은기준일을순서대로처리하고호출사이에간격을둔다() {
        givenClaims(date(FIRST_DATE, 1), date(FIRST_DATE.plusDays(1), 1));
        givenCollected();

        int processed = runner.run(JOB_ID);

        assertThat(processed).isEqualTo(2);

        ArgumentCaptor<CollectOverallRankingSnapshotRequest> requestCaptor =
                ArgumentCaptor.forClass(
                        CollectOverallRankingSnapshotRequest.class
                );

        verify(collectUseCase, times(2)).collect(requestCaptor.capture());

        assertThat(requestCaptor.getAllValues())
                .extracting(
                        request -> request.rankingDate(),
                        request -> request.maxPages()
                )
                .containsExactly(
                        org.assertj.core.api.Assertions.tuple(
                                FIRST_DATE, MAX_PAGES
                        ),
                        org.assertj.core.api.Assertions.tuple(
                                FIRST_DATE.plusDays(1), MAX_PAGES
                        )
                );

        // 첫 호출 앞에서는 기다리지 않는다.
        assertThat(sleeper.sleeps).containsExactly(Duration.ofSeconds(2));

        verify(backfillStateService, times(2)).succeedDate(any());
    }

    @Test
    void 실행을시작할때중단된점유를회수한다() {
        BackfillDate stale = date(FIRST_DATE, 1);

        given(backfillStateService.findStaleClaims(
                org.mockito.ArgumentMatchers.eq(JOB_ID),
                any()
        )).willReturn(List.of(stale));
        given(backfillStateService.claimNextPendingDate(JOB_ID))
                .willReturn(Optional.empty());

        runner.run(JOB_ID);

        // 회수는 재시도 가능한 실패로 기록해 다시 점유되게 한다.
        verify(backfillStateService).failDate(
                org.mockito.ArgumentMatchers.eq(stale.id()),
                org.mockito.ArgumentMatchers.eq(BackfillErrorType.UNKNOWN),
                org.mockito.ArgumentMatchers.eq(true)
        );
    }

    @Test
    void 회수도시도한도를넘으면최종실패로닫는다() {
        BackfillDate stale = date(FIRST_DATE, MAX_ATTEMPTS);

        given(backfillStateService.findStaleClaims(
                org.mockito.ArgumentMatchers.eq(JOB_ID),
                any()
        )).willReturn(List.of(stale));
        given(backfillStateService.claimNextPendingDate(JOB_ID))
                .willReturn(Optional.empty());

        runner.run(JOB_ID);

        // 매번 죽는 기준일이 무한히 회수되지 않는다.
        verify(backfillStateService).failDate(
                org.mockito.ArgumentMatchers.eq(stale.id()),
                org.mockito.ArgumentMatchers.eq(BackfillErrorType.UNKNOWN),
                org.mockito.ArgumentMatchers.eq(false)
        );
    }

    @Test
    void 회수하나가실패해도남은기준일처리를막지않는다() {
        BackfillDate failing = date(FIRST_DATE, 1);
        BackfillDate remaining = date(FIRST_DATE.plusDays(1), 1);

        given(backfillStateService.findStaleClaims(
                org.mockito.ArgumentMatchers.eq(JOB_ID),
                any()
        )).willReturn(List.of(failing, remaining));
        org.mockito.BDDMockito.willThrow(new IllegalStateException("회수 실패"))
                .given(backfillStateService)
                .failDate(
                        org.mockito.ArgumentMatchers.eq(failing.id()),
                        any(),
                        org.mockito.ArgumentMatchers.anyBoolean()
                );

        givenClaims(date(FIRST_DATE.plusDays(2), 1));
        givenCollected();

        assertThat(runner.run(JOB_ID)).isEqualTo(1);

        // 실패한 항목에서 끊지 않고 남은 회수와 수집을 모두 진행한다.
        verify(backfillStateService).failDate(
                org.mockito.ArgumentMatchers.eq(remaining.id()),
                org.mockito.ArgumentMatchers.eq(BackfillErrorType.UNKNOWN),
                org.mockito.ArgumentMatchers.eq(true)
        );
        verify(backfillStateService).succeedDate(any());
    }

    @Test
    void 회수임계시각은설정값만큼과거다() {
        given(backfillStateService.findStaleClaims(
                org.mockito.ArgumentMatchers.eq(JOB_ID),
                any()
        )).willReturn(List.of());
        given(backfillStateService.claimNextPendingDate(JOB_ID))
                .willReturn(Optional.empty());

        Instant before = Instant.now().minus(STALE_CLAIM_TIMEOUT);

        runner.run(JOB_ID);

        ArgumentCaptor<Instant> claimedBeforeCaptor =
                ArgumentCaptor.forClass(Instant.class);

        verify(backfillStateService).findStaleClaims(
                org.mockito.ArgumentMatchers.eq(JOB_ID),
                claimedBeforeCaptor.capture()
        );

        Instant after = Instant.now().minus(STALE_CLAIM_TIMEOUT);

        assertThat(claimedBeforeCaptor.getValue())
                .isBetween(before, after);
    }

    @Test
    void 이미수집된기준일은Skip으로기록한다() {
        givenClaims(date(FIRST_DATE, 1));
        given(collectUseCase.collect(any()))
                .willReturn(outcome(OverallRankingCollectionStatus.SKIPPED));

        runner.run(JOB_ID);

        verify(backfillStateService).skipDate(any());
        verify(backfillStateService, never()).succeedDate(any());
    }

    @Test
    void 한번의실행은기준일상한까지만처리한다() {
        runner = createRunner(1);

        givenClaims(date(FIRST_DATE, 1), date(FIRST_DATE.plusDays(1), 1));
        givenCollected();

        assertThat(runner.run(JOB_ID)).isEqualTo(1);

        verify(collectUseCase, times(1)).collect(any());
        verify(backfillStateService, times(1)).claimNextPendingDate(JOB_ID);
    }

    @Test
    void 남은기준일이없으면외부호출없이끝난다() {
        given(backfillStateService.claimNextPendingDate(JOB_ID))
                .willReturn(Optional.empty());

        assertThat(runner.run(JOB_ID)).isZero();

        verifyNoInteractions(collectUseCase);
        assertThat(sleeper.sleeps).isEmpty();
    }

    static Stream<Arguments> 실패분류표() {
        return Stream.of(
                Arguments.of(
                        OverallRankingCollectionFailure
                                .EXTERNAL_API_SERVER_ERROR,
                        BackfillErrorType.EXTERNAL_SERVER,
                        true
                ),
                Arguments.of(
                        OverallRankingCollectionFailure.EXTERNAL_API_TIMEOUT,
                        BackfillErrorType.EXTERNAL_TIMEOUT,
                        true
                ),
                Arguments.of(
                        OverallRankingCollectionFailure
                                .EXTERNAL_API_CLIENT_ERROR,
                        BackfillErrorType.EXTERNAL_CLIENT,
                        false
                ),
                Arguments.of(
                        OverallRankingCollectionFailure
                                .EXTERNAL_API_RESPONSE_INVALID,
                        BackfillErrorType.RESPONSE_INVALID,
                        false
                )
        );
    }

    @ParameterizedTest
    @MethodSource("실패분류표")
    void 수집실패를분류하고재시도여부를판정한다(
            OverallRankingCollectionFailure failure,
            BackfillErrorType expectedErrorType,
            boolean expectedRetryable
    ) {
        givenClaims(date(FIRST_DATE, 1));
        given(collectUseCase.collect(any()))
                .willThrow(new OverallRankingCollectionException(failure));

        runner.run(JOB_ID);

        verify(backfillStateService).failDate(
                any(),
                org.mockito.ArgumentMatchers.eq(expectedErrorType),
                org.mockito.ArgumentMatchers.eq(expectedRetryable)
        );
    }

    /**
     * 한도 초과를 만나면 이번 실행을 멈춘다.
     *
     * 한도가 마른 상태에서 다음 기준일로 넘어가도 똑같이 실패한다. 실제로 그렇게
     * 630회를 헛되이 썼다.
     */
    @Test
    void 한도초과를만나면이번실행을멈춘다() {
        givenClaims(
                date(FIRST_DATE, 1),
                date(FIRST_DATE.plusDays(1), 1)
        );

        given(collectUseCase.collect(any()))
                .willThrow(new OverallRankingCollectionException(
                        OverallRankingCollectionFailure
                                .EXTERNAL_API_RATE_LIMITED
                ));

        int processed = runner.run(JOB_ID);

        assertThat(processed).isEqualTo(1);

        // 두 번째 기준일은 건드리지 않는다.
        verify(collectUseCase, org.mockito.Mockito.times(1)).collect(any());
    }

    /**
     * 한도 초과는 그 기준일의 시도 횟수를 쓰지 않는다.
     *
     * 점유할 때마다 시도가 오르는데, 한도가 이어지는 동안 같은 기준일을 다시 잡으면
     * 몇 초 만에 한도를 다 써 영구 실패가 된다. 한도 초과는 그 기준일을 시험한 적이
     * 없으므로 세지 않는다.
     */
    @Test
    void 한도초과는시도한도와무관하게재시도가능하다() {
        // 이미 시도 한도에 닿은 기준일이다.
        givenClaims(date(FIRST_DATE, 3));

        given(collectUseCase.collect(any()))
                .willThrow(new OverallRankingCollectionException(
                        OverallRankingCollectionFailure
                                .EXTERNAL_API_RATE_LIMITED
                ));

        runner.run(JOB_ID);

        // 실패로 기록하면 점유할 때 오른 시도가 남는다. 되돌려야 한다.
        verify(backfillStateService).releaseDate(
                any(),
                org.mockito.ArgumentMatchers
                        .eq(BackfillErrorType.EXTERNAL_RATE_LIMITED)
        );

        verify(backfillStateService, org.mockito.Mockito.never())
                .failDate(
                        any(),
                        any(),
                        org.mockito.ArgumentMatchers.anyBoolean()
                );
    }

    @Test
    void 시도횟수가한도에닿으면재시도가능한오류도멈춘다() {
        givenClaims(date(FIRST_DATE, MAX_ATTEMPTS));
        given(collectUseCase.collect(any()))
                .willThrow(new OverallRankingCollectionException(
                        OverallRankingCollectionFailure
                                .EXTERNAL_API_TIMEOUT
                ));

        runner.run(JOB_ID);

        verify(backfillStateService).failDate(
                any(),
                org.mockito.ArgumentMatchers.eq(
                        BackfillErrorType.EXTERNAL_TIMEOUT
                ),
                org.mockito.ArgumentMatchers.eq(false)
        );
    }

    @Test
    void 정기수집과겹치면재시도가능한실패로기록한다() {
        givenClaims(date(FIRST_DATE, 1));
        given(collectUseCase.collect(any()))
                .willThrow(
                        new OverallRankingCollectionAlreadyRunningException()
                );

        runner.run(JOB_ID);

        verify(backfillStateService).failDate(
                any(),
                org.mockito.ArgumentMatchers.eq(
                        BackfillErrorType.EXTERNAL_SERVER
                ),
                org.mockito.ArgumentMatchers.eq(true)
        );
    }

    @Test
    void 저장실패는재시도가능한STORE_FAILED로기록한다() {
        givenClaims(date(FIRST_DATE, 1));
        given(collectUseCase.collect(any()))
                .willThrow(new IllegalStateException("저장 실패"));

        runner.run(JOB_ID);

        verify(backfillStateService).failDate(
                any(),
                org.mockito.ArgumentMatchers.eq(
                        BackfillErrorType.STORE_FAILED
                ),
                org.mockito.ArgumentMatchers.eq(true)
        );
    }

    @Test
    void 실패한기준일뒤에도남은기준일을계속처리한다() {
        givenClaims(date(FIRST_DATE, 1), date(FIRST_DATE.plusDays(1), 1));
        given(collectUseCase.collect(any()))
                .willThrow(new OverallRankingCollectionException(
                        OverallRankingCollectionFailure
                                .EXTERNAL_API_TIMEOUT
                ))
                .willReturn(
                        outcome(OverallRankingCollectionStatus.COLLECTED)
                );

        assertThat(runner.run(JOB_ID)).isEqualTo(2);

        verify(backfillStateService).failDate(any(), any(), any(Boolean.class));
        verify(backfillStateService).succeedDate(any());
    }

    @Test
    void 대기중중단되면점유한기준일을풀고예외를올린다() {
        BackfillDate firstDate = date(FIRST_DATE, 1);
        BackfillDate secondDate = date(FIRST_DATE.plusDays(1), 1);

        givenClaims(firstDate, secondDate);
        givenCollected();

        sleeper.interrupt = true;

        try {
            org.assertj.core.api.Assertions
                    .assertThatThrownBy(() -> runner.run(JOB_ID))
                    .isInstanceOf(IllegalStateException.class);

            // 두 번째 기준일을 점유한 뒤 중단됐다. RUNNING으로 남기면 다시 잡히지 않는다.
            verify(backfillStateService).failDate(
                    org.mockito.ArgumentMatchers.eq(secondDate.id()),
                    org.mockito.ArgumentMatchers.eq(
                            BackfillErrorType.UNKNOWN
                    ),
                    org.mockito.ArgumentMatchers.eq(true)
            );

            // 첫 기준일은 정상 처리됐다.
            verify(backfillStateService)
                    .succeedDate(
                            org.mockito.ArgumentMatchers.eq(firstDate.id())
                    );
            verify(collectUseCase, times(1)).collect(any());
        } finally {
            // 다른 테스트에 Interrupt 상태를 넘기지 않는다.
            Thread.interrupted();
        }
    }

    private OverallRankingBackfillRunner createRunner(int maxDatesPerRun) {
        return new OverallRankingBackfillRunner(
                backfillStateService,
                collectUseCase,
                new OverallRankingBackfillProperties(
                        MAX_PAGES,
                        MAX_ATTEMPTS,
                        maxDatesPerRun,
                        Duration.ofSeconds(2),
                        STALE_CLAIM_TIMEOUT
                ),
                sleeper
        );
    }

    private void givenClaims(BackfillDate... dates) {
        Deque<Optional<BackfillDate>> claims = new ArrayDeque<>(
                Stream.of(dates)
                        .map(date -> Optional.of(date))
                        .toList()
        );

        given(backfillStateService.claimNextPendingDate(JOB_ID))
                .willAnswer(invocation -> claims.isEmpty()
                        ? Optional.empty()
                        : claims.poll());
    }

    private void givenCollected() {
        given(collectUseCase.collect(any()))
                .willReturn(
                        outcome(OverallRankingCollectionStatus.COLLECTED)
                );
    }

    private CollectOverallRankingSnapshotOutcome outcome(
            OverallRankingCollectionStatus status
    ) {
        return new CollectOverallRankingSnapshotOutcome(
                status,
                FIRST_DATE,
                1,
                10,
                false
        );
    }

    private BackfillDate date(
            LocalDate snapshotDate,
            int attemptCount
    ) {
        return new BackfillDate(
                UUID.randomUUID(),
                JOB_ID,
                snapshotDate,
                BackfillStatus.RUNNING,
                attemptCount,
                null,
                null,
                null
        );
    }

    /**
     * 실제로 기다리지 않고 요청된 간격만 기록한다.
     */
    private static final class RecordingSleeper
            implements OverallRankingBackfillRunner.Sleeper {

        private final List<Duration> sleeps = new java.util.ArrayList<>();

        private boolean interrupt;

        @Override
        public void sleep(Duration duration) throws InterruptedException {
            if (interrupt) {
                throw new InterruptedException("중단");
            }

            sleeps.add(duration);
        }
    }
}
