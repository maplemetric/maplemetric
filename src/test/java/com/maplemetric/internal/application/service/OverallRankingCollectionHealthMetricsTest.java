package com.maplemetric.internal.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.maplemetric.internal.application.properties.OverallRankingGapRecoveryProperties;
import com.maplemetric.ranking.api.FindMissingOverallRankingDatesUseCase;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 수집이 며칠째 빠졌는지를 밖에서 볼 수 있는지 확인한다.
 *
 * 이 값을 보고 사람이 손을 댈지 판단한다. 잘못 세면 문제가 없다고 읽히거나, 없는
 * 문제를 있다고 알린다.
 */
class OverallRankingCollectionHealthMetricsTest {

    private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 29);

    private static final LocalDate YESTERDAY = TODAY.minusDays(1);

    private static final int LOOKBACK_DAYS = 30;

    private final FindMissingOverallRankingDatesUseCase findMissingUseCase =
            mock(FindMissingOverallRankingDatesUseCase.class);

    private final MeterRegistry meterRegistry = new SimpleMeterRegistry();

    @Test
    void 되짚는기간의빈날수를내보낸다() {
        givenMissing(YESTERDAY.minusDays(3), YESTERDAY.minusDays(5));

        assertThat(gaugeValue("ranking.collection.missing.days"))
                .isEqualTo(2.0);
    }

    /**
     * 어제까지 받았으면 밀린 날이 없다.
     *
     * 그 앞에 빈 날이 있어도 마지막 수집은 어제다. 두 값을 하나로 합치면 "며칠째
     * 안 들어온다"와 "예전에 몇 개 비었다"를 구분하지 못한다.
     */
    @Test
    void 어제까지받았으면밀린날이없다() {
        givenMissing(YESTERDAY.minusDays(5));

        assertThat(gaugeValue("ranking.collection.staleness.days"))
                .isEqualTo(0.0);
    }

    /** 어제부터 거꾸로 이어진 빈 날의 수가 밀린 날이다. */
    @Test
    void 어제부터이어진빈날수를밀린날로센다() {
        givenMissing(
                YESTERDAY,
                YESTERDAY.minusDays(1),
                YESTERDAY.minusDays(2),
                YESTERDAY.minusDays(4)
        );

        assertThat(gaugeValue("ranking.collection.staleness.days"))
                .isEqualTo(3.0);
    }

    @Test
    void 되짚는기간은어제까지다() {
        givenMissing();

        gaugeValue("ranking.collection.missing.days");

        verify(findMissingUseCase).findMissingDates(
                eq(TODAY.minusDays(LOOKBACK_DAYS)),
                eq(YESTERDAY)
        );
    }

    /**
     * 셀 수 없으면 0이 아니라 -1로 알린다.
     *
     * 0으로 두면 "빠진 날이 없다"로 읽혀, 정작 아무것도 못 세고 있는 동안 아무도
     * 알아채지 못한다.
     */
    @Test
    void 셀수없으면모른다는값을내보낸다() {
        willThrow(new IllegalStateException("저장소를 읽지 못했다"))
                .given(findMissingUseCase)
                .findMissingDates(any(), any());

        assertThat(gaugeValue("ranking.collection.missing.days"))
                .isEqualTo(-1.0);
        assertThat(gaugeValue("ranking.collection.staleness.days"))
                .isEqualTo(-1.0);
    }

    /**
     * 긁어 갈 때마다 다시 세지 않는다.
     *
     * 수집 상태는 하루 단위로 움직이는데 긁는 주기는 그보다 훨씬 짧다. 매번 세면 그
     * 자체가 저장소 부하가 된다.
     */
    @Test
    void 짧은사이에거듭물어도한번만센다() {
        givenMissing(YESTERDAY);

        OverallRankingCollectionHealthMetrics metrics = createMetrics();

        metrics.missingDays();
        metrics.missingDays();
        metrics.stalenessDays();

        verify(findMissingUseCase, times(1))
                .findMissingDates(any(), any());
    }

    private double gaugeValue(String name) {
        createMetrics();

        return meterRegistry.get(name).gauge().value();
    }

    private OverallRankingCollectionHealthMetrics createMetrics() {
        return new OverallRankingCollectionHealthMetrics(
                findMissingUseCase,
                new OverallRankingGapRecoveryProperties(
                        true,
                        LOOKBACK_DAYS,
                        30
                ),
                meterRegistry,
                Clock.fixed(
                        TODAY.atStartOfDay(KOREA_ZONE_ID).toInstant(),
                        KOREA_ZONE_ID
                )
        );
    }

    private void givenMissing(LocalDate... dates) {
        given(findMissingUseCase.findMissingDates(any(), any()))
                .willReturn(List.of(dates));
    }
}
