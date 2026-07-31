package com.maplemetric.statistics.application.result;

import static org.assertj.core.api.Assertions.assertThat;

import com.maplemetric.ranking.api.OverallRankingWorldStatisticsComparisonSnapshot;
import com.maplemetric.ranking.api.OverallRankingWorldStatisticsSnapshot;
import com.maplemetric.ranking.api.OverallRankingWorldStatisticsSnapshot.WorldCount;
import com.maplemetric.statistics.application.result.GetWorldStatisticsResult.WorldComparisonResult;
import com.maplemetric.statistics.application.result.GetWorldStatisticsResult.WorldStatisticsResult;
import com.maplemetric.world.api.CanonicalWorld;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * 월드 Comparison 정책표를 직업과 같은 기준으로 고정한다.
 *
 * 정책 문서가 아니라 테스트가 계약이다. 직업과 월드가 다른 규칙으로 갈라지면
 * 이 테스트와 직업 쪽 테스트가 함께 깨져야 한다.
 */
class WorldComparisonPolicyTest {

    private static final LocalDate ASOF = LocalDate.of(2026, 7, 24);

    private static final LocalDate PREVIOUS_ASOF = LocalDate.of(2026, 7, 21);

    private static final Instant COLLECTED_AT =
            Instant.parse("2026-07-24T01:00:00Z");

    private static final CanonicalWorld LUNA = new CanonicalWorld(
            "luna",
            "루나",
            CanonicalWorld.Status.ACTIVE,
            1
    );

    private static final String LEVEL = "200";

    static List<Arguments> 판정표() {
        return List.of(
                Arguments.of(
                        "이전 Snapshot 없음",
                        null, null, 100, 10L,
                        null, null, null, null, null, null,
                        StatisticsTrend.INSUFFICIENT_DATA
                ),
                Arguments.of(
                        "이전 sampleSize 0",
                        0, 0L, 100, 10L,
                        null, null, null, null, null, null,
                        StatisticsTrend.INSUFFICIENT_DATA
                ),
                Arguments.of(
                        "현재 sampleSize 0",
                        100, 10L, 0, 0L,
                        10L, "10.00", null, null, null, null,
                        StatisticsTrend.INSUFFICIENT_DATA
                ),
                Arguments.of(
                        "이전 Count 0, 현재 Count 0",
                        100, 0L, 100, 0L,
                        0L, "0.00", 0L, null, null, "0.00",
                        StatisticsTrend.STABLE
                ),
                Arguments.of(
                        "이전 Count 0, 현재 Count 초과",
                        100, 0L, 100, 25L,
                        0L, "0.00", 25L, null, null, "25.00",
                        StatisticsTrend.NEW
                ),
                Arguments.of(
                        "이전 Count 초과, 현재 Count 0",
                        100, 20L, 100, 0L,
                        20L, "20.00", -20L, "-100.00", "-100.00", "-20.00",
                        StatisticsTrend.REMOVED
                ),
                Arguments.of(
                        "이전보다 증가",
                        100, 10L, 100, 20L,
                        10L, "10.00", 10L, "100.00", "100.00", "10.00",
                        StatisticsTrend.UP
                ),
                Arguments.of(
                        "이전보다 감소",
                        100, 20L, 100, 10L,
                        20L, "20.00", -10L, "-50.00", "-50.00", "-10.00",
                        StatisticsTrend.DOWN
                ),
                Arguments.of(
                        "점유율 동일",
                        100, 20L, 200, 40L,
                        20L, "20.00", 20L, "100.00", "0.00", "0.00",
                        StatisticsTrend.STABLE
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("판정표")
    void 변화율과Trend판정표를고정한다(
            String 설명,
            Integer previousSampleSize,
            Long previousCount,
            int sampleSize,
            long count,
            Long expectedPreviousCount,
            String expectedPreviousPercentage,
            Long expectedCountChange,
            String expectedCountChangeRate,
            String expectedPercentageChangeRate,
            String expectedPercentagePointChange,
            StatisticsTrend expectedTrend
    ) {
        WorldComparisonResult comparison = comparisonOf(
                previousSampleSize,
                previousCount,
                sampleSize,
                count
        );

        assertThat(comparison.previousCount())
                .isEqualTo(expectedPreviousCount);
        assertThat(comparison.previousPercentage())
                .isEqualTo(decimal(expectedPreviousPercentage));
        assertThat(comparison.countChange()).isEqualTo(expectedCountChange);
        assertThat(comparison.countChangeRate())
                .isEqualTo(decimal(expectedCountChangeRate));
        assertThat(comparison.percentageChangeRate())
                .isEqualTo(decimal(expectedPercentageChangeRate));
        assertThat(comparison.percentagePointChange())
                .isEqualTo(decimal(expectedPercentagePointChange));
        assertThat(comparison.trend()).isEqualTo(expectedTrend);
    }

    @Test
    void 반올림전양수인미세변화는STABLE이다() {
        WorldComparisonResult comparison =
                comparisonOf(1000000, 40000L, 1000000, 40040L);

        assertThat(comparison.percentagePointChange())
                .isEqualTo(new BigDecimal("0.00"));
        assertThat(comparison.trend()).isEqualTo(StatisticsTrend.STABLE);
    }

    @Test
    void 반올림후0으로보이는이전점유율도변화율분모로사용한다() {
        WorldComparisonResult comparison =
                comparisonOf(1000000, 40L, 1000000, 80L);

        assertThat(comparison.previousPercentage())
                .isEqualTo(new BigDecimal("0.00"));
        assertThat(comparison.percentageChangeRate())
                .isEqualTo(new BigDecimal("100.00"));
        assertThat(comparison.countChangeRate())
                .isEqualTo(new BigDecimal("100.00"));
    }

    @Test
    void 변화량은반올림된점유율이아니라원본비율로계산한다() {
        WorldComparisonResult comparison = comparisonOf(7, 1L, 3, 1L);

        assertThat(comparison.previousPercentage())
                .isEqualTo(new BigDecimal("14.29"));
        assertThat(comparison.percentagePointChange())
                .isEqualTo(new BigDecimal("19.05"))
                .isNotEqualTo(
                        new BigDecimal("33.33")
                                .subtract(new BigDecimal("14.29"))
                );
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 100})
    void 표본에없는월드도항상Comparison을가진다(int previousSampleSize) {
        WorldStatisticsResult world =
                worldOf(previousSampleSize, 0L, 100, 0L);

        assertThat(world.count()).isZero();
        assertThat(world.percentage()).isEqualTo(new BigDecimal("0.00"));
        assertThat(world.averageLevel()).isNull();
        assertThat(world.comparison()).isNotNull();
        assertThat(world.comparison().trend()).isNotNull();
    }

    @Test
    void 이전Snapshot없는목록Overload는INSUFFICIENT_DATA다() {
        GetWorldStatisticsResult result = GetWorldStatisticsResult.from(
                snapshot(ASOF, 100, 10L),
                List.of(LUNA),
                Map.of("루나", LUNA)
        );

        assertThat(result.previousAsOf()).isNull();
        assertThat(result.daysBetween()).isNull();
        assertThat(result.worlds().get(0).comparison().trend())
                .isEqualTo(StatisticsTrend.INSUFFICIENT_DATA);
    }

    private WorldComparisonResult comparisonOf(
            Integer previousSampleSize,
            Long previousCount,
            int sampleSize,
            long count
    ) {
        return worldOf(
                previousSampleSize,
                previousCount,
                sampleSize,
                count
        ).comparison();
    }

    private WorldStatisticsResult worldOf(
            Integer previousSampleSize,
            Long previousCount,
            int sampleSize,
            long count
    ) {
        OverallRankingWorldStatisticsSnapshot previous =
                previousSampleSize == null
                        ? null
                        : snapshot(
                                PREVIOUS_ASOF,
                                previousSampleSize,
                                previousCount
                        );

        GetWorldStatisticsResult result = GetWorldStatisticsResult.from(
                new OverallRankingWorldStatisticsComparisonSnapshot(
                        snapshot(ASOF, sampleSize, count),
                        previous,
                        previous == null ? null : 3
                ),
                List.of(LUNA),
                Map.of("루나", LUNA)
        );

        return result.worlds().get(0);
    }

    private OverallRankingWorldStatisticsSnapshot snapshot(
            LocalDate asOf,
            int sampleSize,
            Long count
    ) {
        List<WorldCount> worldCounts = count == null || count == 0L
                ? List.of()
                : List.of(new WorldCount(
                        "루나",
                        count,
                        new BigDecimal(LEVEL)
                ));

        return new OverallRankingWorldStatisticsSnapshot(
                asOf,
                "NEXON_OPEN_API",
                COLLECTED_AT,
                sampleSize,
                1,
                100,
                false,
                worldCounts
        );
    }

    private BigDecimal decimal(String value) {
        return value == null ? null : new BigDecimal(value);
    }
}
