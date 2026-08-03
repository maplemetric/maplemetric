package com.maplemetric.statistics.application.result;

import com.maplemetric.ranking.api.OverallRankingWorldStatisticsComparisonSnapshot;
import com.maplemetric.ranking.api.OverallRankingWorldStatisticsSnapshot;
import com.maplemetric.statistics.api.StatisticsTrend;
import com.maplemetric.world.api.CanonicalWorld;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record GetWorldStatisticsResult(
        List<WorldStatisticsResult> worlds,
        int sampleSize,
        LocalDate asOf,
        String source,
        Instant collectedAt,
        int pageCount,
        int requestedMaxPages,
        boolean truncated,
        LocalDate previousAsOf,
        Integer daysBetween
) {

    private static final int PERCENTAGE_SCALE = 2;
    private static final int AVERAGE_LEVEL_SCALE = 1;

    /**
     * 중간 나눗셈 정밀도다.
     *
     * 무한소수가 나오는 나눗셈은 정밀도 없이 실행할 수 없으므로 공통 MathContext를 쓴다.
     * 응답 Scale은 여기서 적용하지 않고 마지막 변환 단계에서만 적용한다.
     */
    private static final MathContext CALCULATION_CONTEXT =
            MathContext.DECIMAL128;

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private static final BigDecimal ZERO_PERCENTAGE =
            BigDecimal.ZERO.setScale(PERCENTAGE_SCALE);

    /**
     * 이전 Snapshot 없이 최신만으로 계산한다.
     *
     * Comparison은 비교 대상이 없으므로 INSUFFICIENT_DATA가 된다.
     */
    public static GetWorldStatisticsResult from(
            OverallRankingWorldStatisticsSnapshot snapshot,
            List<CanonicalWorld> canonicalWorlds,
            Map<String, CanonicalWorld> canonicalWorldsByWorldName
    ) {
        return from(
                new OverallRankingWorldStatisticsComparisonSnapshot(
                        snapshot,
                        null,
                        null
                ),
                canonicalWorlds,
                canonicalWorldsByWorldName
        );
    }

    public static GetWorldStatisticsResult from(
            OverallRankingWorldStatisticsComparisonSnapshot comparison,
            List<CanonicalWorld> canonicalWorlds,
            Map<String, CanonicalWorld> canonicalWorldsByWorldName
    ) {
        OverallRankingWorldStatisticsSnapshot latest = comparison.latest();
        OverallRankingWorldStatisticsSnapshot previous = comparison.previous();

        Map<String, CanonicalAggregate> latestAggregates =
                aggregateByWorldSlug(latest, canonicalWorldsByWorldName);

        Map<String, CanonicalAggregate> previousAggregates = previous == null
                ? Map.of()
                : aggregateByWorldSlug(previous, canonicalWorldsByWorldName);

        List<WorldStatisticsResult> worlds = canonicalWorlds.stream()
                .map(canonicalWorld -> toWorldStatisticsResult(
                        canonicalWorld,
                        latestAggregates.get(canonicalWorld.worldSlug()),
                        previousAggregates.get(canonicalWorld.worldSlug()),
                        latest.sampleSize(),
                        previous == null ? null : previous.sampleSize()
                ))
                .toList();

        return new GetWorldStatisticsResult(
                worlds,
                latest.sampleSize(),
                latest.asOf(),
                latest.source(),
                latest.collectedAt(),
                latest.pageCount(),
                latest.requestedMaxPages(),
                latest.truncated(),
                previous == null ? null : previous.asOf(),
                comparison.daysBetween()
        );
    }

    private static Map<String, CanonicalAggregate> aggregateByWorldSlug(
            OverallRankingWorldStatisticsSnapshot snapshot,
            Map<String, CanonicalWorld> canonicalWorldsByWorldName
    ) {
        Map<String, CanonicalAggregate> aggregates = new HashMap<>();

        snapshot.worldCounts().forEach(worldCount -> {
            CanonicalWorld canonicalWorld =
                    canonicalWorldsByWorldName.get(worldCount.worldName());

            if (canonicalWorld == null) {
                return;
            }

            aggregates.merge(
                    canonicalWorld.worldSlug(),
                    CanonicalAggregate.of(
                            worldCount.count(),
                            worldCount.averageLevel()
                    ),
                    (existing, added) -> existing.plus(added)
            );
        });

        return aggregates;
    }

    private static WorldStatisticsResult toWorldStatisticsResult(
            CanonicalWorld canonicalWorld,
            CanonicalAggregate latestAggregate,
            CanonicalAggregate previousAggregate,
            int sampleSize,
            Integer previousSampleSize
    ) {
        long count = latestAggregate == null ? 0L : latestAggregate.count();

        BigDecimal rawPercentage = rawPercentage(count, sampleSize);

        BigDecimal percentage = rawPercentage == null
                ? ZERO_PERCENTAGE
                : toPercentageScale(rawPercentage);

        return new WorldStatisticsResult(
                canonicalWorld.worldSlug(),
                canonicalWorld.worldName(),
                count,
                percentage,
                latestAggregate == null
                        ? null
                        : latestAggregate.averageLevel(),
                toComparison(
                        count,
                        rawPercentage,
                        previousAggregate,
                        previousSampleSize
                )
        );
    }

    private static WorldComparisonResult toComparison(
            long count,
            BigDecimal rawPercentage,
            CanonicalAggregate previousAggregate,
            Integer previousSampleSize
    ) {
        if (previousSampleSize == null || previousSampleSize <= 0) {
            return WorldComparisonResult.insufficient(null, null);
        }

        long previousCount = previousAggregate == null
                ? 0L
                : previousAggregate.count();

        BigDecimal rawPreviousPercentage = rawPercentage(
                previousCount,
                previousSampleSize
        );

        BigDecimal previousPercentage =
                toPercentageScale(rawPreviousPercentage);

        if (rawPercentage == null) {
            return WorldComparisonResult.insufficient(
                    previousCount,
                    previousPercentage
            );
        }

        BigDecimal percentagePointChange = toPercentageScale(
                rawPercentage.subtract(rawPreviousPercentage)
        );

        return new WorldComparisonResult(
                previousCount,
                previousPercentage,
                count - previousCount,
                toChangeRate(
                        BigDecimal.valueOf(count),
                        BigDecimal.valueOf(previousCount)
                ),
                toChangeRate(rawPercentage, rawPreviousPercentage),
                percentagePointChange,
                toTrend(count, previousCount, percentagePointChange)
        );
    }

    /**
     * 상대 변화율이다.
     *
     * 분모가 0인지는 반올림 전 값으로 판정한다. 응답에서 0.00으로 보인다는 이유로
     * 분모를 0으로 처리하지 않는다.
     */
    private static BigDecimal toChangeRate(
            BigDecimal current,
            BigDecimal previous
    ) {
        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        return toPercentageScale(
                current.subtract(previous)
                        .multiply(HUNDRED)
                        .divide(previous, CALCULATION_CONTEXT)
        );
    }

    /**
     * Trend 판정이다.
     *
     * NEW와 REMOVED는 Count로 먼저 판정한다. UP·DOWN·STABLE은 응답용으로 반올림한
     * percentagePointChange의 부호로 판정해, 0.00으로 표시되는 미세 변화가
     * 상승·하락으로 보이지 않게 한다.
     */
    private static StatisticsTrend toTrend(
            long count,
            long previousCount,
            BigDecimal percentagePointChange
    ) {
        if (previousCount == 0L && count > 0L) {
            return StatisticsTrend.NEW;
        }

        if (previousCount > 0L && count == 0L) {
            return StatisticsTrend.REMOVED;
        }

        int signum = percentagePointChange.signum();

        if (signum > 0) {
            return StatisticsTrend.UP;
        }

        if (signum < 0) {
            return StatisticsTrend.DOWN;
        }

        return StatisticsTrend.STABLE;
    }

    private static BigDecimal rawPercentage(
            long count,
            int sampleSize
    ) {
        if (sampleSize <= 0) {
            return null;
        }

        return BigDecimal.valueOf(count)
                .multiply(HUNDRED)
                .divide(BigDecimal.valueOf(sampleSize), CALCULATION_CONTEXT);
    }

    private static BigDecimal toPercentageScale(BigDecimal value) {
        return value.setScale(PERCENTAGE_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 같은 Canonical 월드에 매칭된 Alias들의 Count 합과 레벨 총합이다.
     *
     * 레벨 총합은 Alias별 평균 레벨에 Count를 곱해 누적하므로,
     * 평균 레벨은 Alias 평균의 단순 평균이 아니라 Count 가중 평균이 된다.
     */
    private record CanonicalAggregate(
            long count,
            BigDecimal levelSum
    ) {

        private static CanonicalAggregate of(
                long count,
                BigDecimal averageLevel
        ) {
            return new CanonicalAggregate(
                    count,
                    averageLevel.multiply(BigDecimal.valueOf(count))
            );
        }

        private CanonicalAggregate plus(CanonicalAggregate added) {
            return new CanonicalAggregate(
                    count + added.count(),
                    levelSum.add(added.levelSum())
            );
        }

        private BigDecimal averageLevel() {
            if (count <= 0L) {
                return null;
            }

            return levelSum.divide(
                    BigDecimal.valueOf(count),
                    AVERAGE_LEVEL_SCALE,
                    RoundingMode.HALF_UP
            );
        }
    }

    public record WorldStatisticsResult(
            String worldSlug,
            String worldName,
            long count,
            BigDecimal percentage,
            BigDecimal averageLevel,
            WorldComparisonResult comparison
    ) {
    }

    public record WorldComparisonResult(
            Long previousCount,
            BigDecimal previousPercentage,
            Long countChange,
            BigDecimal countChangeRate,
            BigDecimal percentageChangeRate,
            BigDecimal percentagePointChange,
            StatisticsTrend trend
    ) {

        private static WorldComparisonResult insufficient(
                Long previousCount,
                BigDecimal previousPercentage
        ) {
            return new WorldComparisonResult(
                    previousCount,
                    previousPercentage,
                    null,
                    null,
                    null,
                    null,
                    StatisticsTrend.INSUFFICIENT_DATA
            );
        }
    }
}
