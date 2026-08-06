package com.maplemetric.statistics.application.result;

import com.maplemetric.ranking.api.OverallRankingWorldStatisticsComparisonSnapshot;
import com.maplemetric.ranking.api.OverallRankingWorldStatisticsSnapshot;
import com.maplemetric.statistics.api.StatisticsDataAvailability;
import com.maplemetric.statistics.application.result.GetWorldStatisticsResult.WorldComparisonResult;
import com.maplemetric.statistics.application.result.GetWorldStatisticsResult.WorldStatisticsResult;
import com.maplemetric.world.api.CanonicalWorld;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

public record GetWorldStatisticsHistoryResult(
        WorldResult world,
        StatisticsDataAvailability dataAvailability,
        RangeResult range,
        RangeComparisonResult rangeComparison,
        List<PointResult> points,
        List<String> limitations
) {

    public static GetWorldStatisticsHistoryResult available(
            CanonicalWorld canonicalWorld,
            String preset,
            LocalDate requestedFrom,
            LocalDate requestedTo,
            List<OverallRankingWorldStatisticsSnapshot> snapshots,
            Map<String, CanonicalWorld> canonicalWorldsByWorldName
    ) {
        List<PointResult> points = snapshots.stream()
                .map(snapshot -> toPoint(
                        canonicalWorld,
                        snapshot,
                        canonicalWorldsByWorldName
                ))
                .toList();

        LocalDate firstAsOf = points.isEmpty()
                ? null
                : points.get(0).asOf();

        LocalDate lastAsOf = points.isEmpty()
                ? null
                : points.get(points.size() - 1).asOf();

        // 보존된 수집이 하나도 없는 전체 기간 요청은 범위 자체가 비어 있다.
        int requestedDateCount = requestedFrom == null || requestedTo == null
                ? 0
                : Math.toIntExact(
                        ChronoUnit.DAYS.between(requestedFrom, requestedTo) + 1
                );

        return new GetWorldStatisticsHistoryResult(
                WorldResult.from(canonicalWorld),
                StatisticsDataAvailability.AVAILABLE,
                new RangeResult(
                        preset,
                        requestedFrom,
                        requestedTo,
                        firstAsOf,
                        lastAsOf,
                        points.size(),
                        requestedDateCount - points.size()
                ),
                toRangeComparison(
                        canonicalWorld,
                        snapshots,
                        canonicalWorldsByWorldName
                ),
                points,
                StatisticsLimitations.HISTORY
        );
    }

    public static GetWorldStatisticsHistoryResult notCollected(
            CanonicalWorld canonicalWorld,
            String preset,
            LocalDate requestedFrom,
            LocalDate requestedTo
    ) {
        int missingDateCount =
                requestedFrom == null || requestedTo == null
                        ? 0
                        : Math.toIntExact(
                                ChronoUnit.DAYS.between(
                                        requestedFrom,
                                        requestedTo
                                ) + 1
                        );

        return new GetWorldStatisticsHistoryResult(
                WorldResult.from(canonicalWorld),
                StatisticsDataAvailability.NOT_COLLECTED,
                new RangeResult(
                        preset,
                        requestedFrom,
                        requestedTo,
                        null,
                        null,
                        0,
                        missingDateCount
                ),
                null,
                List.of(),
                StatisticsLimitations.HISTORY
        );
    }

    private static PointResult toPoint(
            CanonicalWorld canonicalWorld,
            OverallRankingWorldStatisticsSnapshot snapshot,
            Map<String, CanonicalWorld> canonicalWorldsByWorldName
    ) {
        WorldStatisticsResult statistics = calculate(
                canonicalWorld,
                new OverallRankingWorldStatisticsComparisonSnapshot(
                        snapshot,
                        null,
                        null
                ),
                canonicalWorldsByWorldName
        );

        return new PointResult(
                snapshot.asOf(),
                statistics.count(),
                statistics.percentage(),
                statistics.averageLevel(),
                snapshot.sampleSize(),
                snapshot.pageCount(),
                snapshot.requestedMaxPages(),
                snapshot.truncated(),
                snapshot.collectedAt()
        );
    }

    private static RangeComparisonResult toRangeComparison(
            CanonicalWorld canonicalWorld,
            List<OverallRankingWorldStatisticsSnapshot> snapshots,
            Map<String, CanonicalWorld> canonicalWorldsByWorldName
    ) {
        if (snapshots.size() < 2) {
            return null;
        }

        OverallRankingWorldStatisticsSnapshot first = snapshots.get(0);
        OverallRankingWorldStatisticsSnapshot last =
                snapshots.get(snapshots.size() - 1);

        int daysBetween = Math.toIntExact(
                ChronoUnit.DAYS.between(first.asOf(), last.asOf())
        );

        WorldStatisticsResult current = calculate(
                canonicalWorld,
                new OverallRankingWorldStatisticsComparisonSnapshot(
                        last,
                        first,
                        daysBetween
                ),
                canonicalWorldsByWorldName
        );

        WorldComparisonResult comparison = current.comparison();

        return new RangeComparisonResult(
                first.asOf(),
                last.asOf(),
                comparison.previousCount(),
                current.count(),
                comparison.countChange(),
                comparison.countChangeRate(),
                comparison.previousPercentage(),
                current.percentage(),
                comparison.percentageChangeRate(),
                comparison.percentagePointChange()
        );
    }

    private static WorldStatisticsResult calculate(
            CanonicalWorld canonicalWorld,
            OverallRankingWorldStatisticsComparisonSnapshot comparison,
            Map<String, CanonicalWorld> canonicalWorldsByWorldName
    ) {
        return GetWorldStatisticsResult.from(
                comparison,
                List.of(canonicalWorld),
                canonicalWorldsByWorldName
        ).worlds().get(0);
    }

    public record WorldResult(
            String worldSlug,
            String worldName
    ) {

        private static WorldResult from(CanonicalWorld canonicalWorld) {
            return new WorldResult(
                    canonicalWorld.worldSlug(),
                    canonicalWorld.worldName()
            );
        }
    }

    public record RangeResult(
            String preset,
            LocalDate requestedFrom,
            LocalDate requestedTo,
            LocalDate firstAsOf,
            LocalDate lastAsOf,
            int pointCount,
            int missingDateCount
    ) {
    }

    public record RangeComparisonResult(
            LocalDate fromAsOf,
            LocalDate toAsOf,
            Long previousCount,
            long currentCount,
            Long countChange,
            BigDecimal countChangeRate,
            BigDecimal previousPercentage,
            BigDecimal currentPercentage,
            BigDecimal percentageChangeRate,
            BigDecimal percentagePointChange
    ) {
    }

    public record PointResult(
            LocalDate asOf,
            long count,
            BigDecimal percentage,
            BigDecimal averageLevel,
            int sampleSize,
            int pageCount,
            int requestedMaxPages,
            boolean truncated,
            Instant collectedAt
    ) {
    }
}
