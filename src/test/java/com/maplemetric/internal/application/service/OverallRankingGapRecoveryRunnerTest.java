package com.maplemetric.internal.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.maplemetric.internal.application.properties.OverallRankingCollectionProperties;
import com.maplemetric.internal.application.properties.OverallRankingGapRecoveryProperties;
import com.maplemetric.ranking.api.CollectOverallRankingSnapshotOutcome;
import com.maplemetric.ranking.api.CollectOverallRankingSnapshotRequest;
import com.maplemetric.ranking.api.CollectOverallRankingSnapshotUseCase;
import com.maplemetric.ranking.api.FindMissingOverallRankingDatesUseCase;
import com.maplemetric.ranking.api.OverallRankingCollectionAlreadyRunningException;
import com.maplemetric.ranking.api.OverallRankingCollectionException;
import com.maplemetric.ranking.api.OverallRankingCollectionFailure;
import com.maplemetric.ranking.api.OverallRankingCollectionRequestClass;
import com.maplemetric.ranking.api.OverallRankingCollectionStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * 비어 있는 기준일을 메우는 동작을 확인한다.
 *
 * 이 경로는 승인 없이 외부를 호출한다. 그래서 무엇을 대상으로 삼는지, 얼마나
 * 호출하는지, 실패했을 때 어디까지 번지는지를 값으로 고정한다.
 */
class OverallRankingGapRecoveryRunnerTest {

    private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 29);

    private static final int LOOKBACK_DAYS = 730;

    private static final int MAX_DATES_PER_RUN = 3;

    private static final int MAX_PAGES = 10;

    private final FindMissingOverallRankingDatesUseCase findMissingUseCase =
            mock(FindMissingOverallRankingDatesUseCase.class);

    private final CollectOverallRankingSnapshotUseCase collectUseCase =
            mock(CollectOverallRankingSnapshotUseCase.class);

    /**
     * 되짚어 볼 기간은 어제까지다.
     *
     * 오늘은 아직 외부에 올라오지 않았을 수 있다. 오늘을 비었다고 보고 받으러 가면
     * 매번 헛걸음한다.
     */
    @Test
    void 어제까지되짚어본다() {
        givenMissing();

        runner(true).run();

        verify(findMissingUseCase).findMissingDates(
                eq(TODAY.minusDays(LOOKBACK_DAYS)),
                eq(TODAY.minusDays(1))
        );
    }

    /**
     * 오래된 기준일부터 메운다.
     *
     * 외부가 이력을 주는 기간이 정해져 있어 오래된 날이 먼저 사라진다. 최근 것부터
     * 메우면 곧 받을 수 없게 될 날을 놓친다.
     */
    @Test
    void 오래된기준일부터한도까지메운다() {
        LocalDate oldest = LocalDate.of(2025, 1, 1);

        givenMissing(
                oldest,
                oldest.plusDays(1),
                oldest.plusDays(2),
                oldest.plusDays(3),
                oldest.plusDays(4)
        );
        givenCollected();

        assertThat(runner(true).run()).isEqualTo(MAX_DATES_PER_RUN);

        ArgumentCaptor<CollectOverallRankingSnapshotRequest> captor =
                ArgumentCaptor.forClass(
                        CollectOverallRankingSnapshotRequest.class
                );

        verify(collectUseCase, times(MAX_DATES_PER_RUN))
                .collect(captor.capture());

        assertThat(captor.getAllValues())
                .extracting(request -> request.rankingDate())
                .containsExactly(
                        oldest,
                        oldest.plusDays(1),
                        oldest.plusDays(2)
                );
    }

    /**
     * 메우기는 대량 요청 몫으로 호출한다.
     *
     * 정기 수집과 같은 몫을 쓰면 여러 날을 메우는 사이 그날의 호출 한도를 먹는다.
     */
    @Test
    void 대량요청몫으로호출한다() {
        givenMissing(LocalDate.of(2025, 1, 1));
        givenCollected();

        runner(true).run();

        ArgumentCaptor<CollectOverallRankingSnapshotRequest> captor =
                ArgumentCaptor.forClass(
                        CollectOverallRankingSnapshotRequest.class
                );

        verify(collectUseCase).collect(captor.capture());

        assertThat(captor.getValue().requestClass())
                .isEqualTo(OverallRankingCollectionRequestClass.BULK);
        assertThat(captor.getValue().maxPages()).isEqualTo(MAX_PAGES);
    }

    /** 꺼 두면 조회조차 하지 않는다. */
    @Test
    void 꺼져있으면아무것도하지않는다() {
        assertThat(runner(false).run()).isZero();

        verifyNoInteractions(findMissingUseCase);
        verifyNoInteractions(collectUseCase);
    }

    /** 빈 날이 없으면 외부를 부르지 않는다. */
    @Test
    void 빈날이없으면호출하지않는다() {
        givenMissing();

        assertThat(runner(true).run()).isZero();

        verify(collectUseCase, never()).collect(any());
    }

    /**
     * 한 기준일이 실패해도 나머지를 멈추지 않는다.
     *
     * 어제 하루가 안 받아진다고 그 앞의 빈 날들까지 못 메울 이유가 없다.
     */
    @Test
    void 한기준일이실패해도나머지를계속메운다() {
        LocalDate failing = LocalDate.of(2025, 1, 1);
        LocalDate following = failing.plusDays(1);

        givenMissing(failing, following);
        givenCollected();

        willThrow(new OverallRankingCollectionException(
                OverallRankingCollectionFailure.EXTERNAL_API_SERVER_ERROR
        ))
                .given(collectUseCase)
                .collect(argumentFor(failing));

        assertThat(runner(true).run()).isEqualTo(1);

        verify(collectUseCase).collect(argumentFor(following));
    }

    /**
     * 다른 수집과 겹치면 이번에는 건너뛴다.
     *
     * 그 기준일의 문제가 아니므로 실패로 세지 않고 다음 실행에서 다시 본다.
     */
    @Test
    void 다른수집과겹치면건너뛴다() {
        LocalDate date = LocalDate.of(2025, 1, 1);

        givenMissing(date);

        willThrow(new OverallRankingCollectionAlreadyRunningException())
                .given(collectUseCase)
                .collect(any());

        assertThat(runner(true).run()).isZero();
    }

    private CollectOverallRankingSnapshotRequest argumentFor(LocalDate date) {
        return new CollectOverallRankingSnapshotRequest(
                date,
                MAX_PAGES,
                OverallRankingCollectionRequestClass.BULK
        );
    }

    private void givenMissing(LocalDate... dates) {
        given(findMissingUseCase.findMissingDates(any(), any()))
                .willReturn(List.of(dates));
    }

    private void givenCollected() {
        given(collectUseCase.collect(any()))
                .willReturn(new CollectOverallRankingSnapshotOutcome(
                        OverallRankingCollectionStatus.COLLECTED,
                        null,
                        1,
                        1,
                        false
                ));
    }

    private OverallRankingGapRecoveryRunner runner(boolean enabled) {
        return new OverallRankingGapRecoveryRunner(
                findMissingUseCase,
                collectUseCase,
                new OverallRankingGapRecoveryProperties(
                        enabled,
                        LOOKBACK_DAYS,
                        MAX_DATES_PER_RUN
                ),
                new OverallRankingCollectionProperties(MAX_PAGES),
                Clock.fixed(
                        TODAY.atStartOfDay(KOREA_ZONE_ID).toInstant()
                                .plus(java.time.Duration.ofHours(9)),
                        KOREA_ZONE_ID
                )
        );
    }
}
