package com.maplemetric.statistics.application.result;

import com.maplemetric.ranking.api.OverallRankingWorldStatisticsComparisonSnapshot;
import com.maplemetric.statistics.api.StatisticsDataAvailability;
import com.maplemetric.statistics.api.StatisticsTrend;
import com.maplemetric.statistics.application.result.GetWorldStatisticsResult.WorldComparisonResult;
import com.maplemetric.statistics.application.result.GetWorldStatisticsResult.WorldStatisticsResult;
import com.maplemetric.world.api.CanonicalWorld;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record GetWorldStatisticsDetailResult(
        WorldResult world,
        StatisticsDataAvailability dataAvailability,
        LatestResult latest,
        ComparisonResult comparison,
        SourceMetaResult sourceMeta
) {

    public static GetWorldStatisticsDetailResult available(
            CanonicalWorld canonicalWorld,
            OverallRankingWorldStatisticsComparisonSnapshot rankingComparison,
            Map<String, CanonicalWorld> canonicalWorldsByWorldName
    ) {
        GetWorldStatisticsResult statistics =
                GetWorldStatisticsResult.from(
                        rankingComparison,
                        List.of(canonicalWorld),
                        canonicalWorldsByWorldName
                );

        WorldStatisticsResult worldStatistics = statistics.worlds().get(0);
        WorldComparisonResult worldComparison = worldStatistics.comparison();

        return new GetWorldStatisticsDetailResult(
                WorldResult.from(canonicalWorld),
                StatisticsDataAvailability.AVAILABLE,
                new LatestResult(
                        statistics.asOf(),
                        worldStatistics.count(),
                        worldStatistics.percentage(),
                        worldStatistics.averageLevel()
                ),
                new ComparisonResult(
                        statistics.previousAsOf(),
                        worldComparison.previousCount(),
                        worldComparison.countChange(),
                        worldComparison.countChangeRate(),
                        worldComparison.previousPercentage(),
                        worldComparison.percentageChangeRate(),
                        worldComparison.percentagePointChange(),
                        worldComparison.trend()
                ),
                new SourceMetaResult(
                        statistics.source(),
                        statistics.collectedAt(),
                        statistics.sampleSize(),
                        statistics.pageCount(),
                        statistics.requestedMaxPages(),
                        statistics.truncated()
                )
        );
    }

    public static GetWorldStatisticsDetailResult notCollected(
            CanonicalWorld canonicalWorld
    ) {
        return new GetWorldStatisticsDetailResult(
                WorldResult.from(canonicalWorld),
                StatisticsDataAvailability.NOT_COLLECTED,
                null,
                null,
                null
        );
    }

    public record WorldResult(
            String worldSlug,
            String worldName,
            CanonicalWorld.Status status,
            int displayOrder
    ) {

        private static WorldResult from(CanonicalWorld canonicalWorld) {
            return new WorldResult(
                    canonicalWorld.worldSlug(),
                    canonicalWorld.worldName(),
                    canonicalWorld.status(),
                    canonicalWorld.displayOrder()
            );
        }
    }

    public record LatestResult(
            LocalDate asOf,
            long count,
            BigDecimal percentage,
            BigDecimal averageLevel
    ) {
    }

    public record ComparisonResult(
            LocalDate previousAsOf,
            Long previousCount,
            Long countChange,
            BigDecimal countChangeRate,
            BigDecimal previousPercentage,
            BigDecimal percentageChangeRate,
            BigDecimal percentagePointChange,
            StatisticsTrend trend
    ) {
    }

    public record SourceMetaResult(
            String source,
            Instant collectedAt,
            int sampleSize,
            int pageCount,
            int requestedMaxPages,
            boolean truncated
    ) {
    }
}
