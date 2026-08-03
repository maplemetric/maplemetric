package com.maplemetric.statistics.presentation.response;

import com.maplemetric.statistics.application.result.GetWorldStatisticsResult;
import com.maplemetric.statistics.application.result.GetWorldStatisticsResult.WorldComparisonResult;
import com.maplemetric.statistics.api.StatisticsTrend;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record GetWorldStatisticsResponse(
        List<WorldStatistics> worlds,
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

    public static GetWorldStatisticsResponse from(
            GetWorldStatisticsResult result
    ) {
        List<WorldStatistics> worlds = result.worlds()
                .stream()
                .map(world -> new WorldStatistics(
                        world.worldSlug(),
                        world.worldName(),
                        world.count(),
                        world.percentage(),
                        world.averageLevel(),
                        toComparison(world.comparison())
                ))
                .toList();

        return new GetWorldStatisticsResponse(
                worlds,
                result.sampleSize(),
                result.asOf(),
                result.source(),
                result.collectedAt(),
                result.pageCount(),
                result.requestedMaxPages(),
                result.truncated(),
                result.previousAsOf(),
                result.daysBetween()
        );
    }

    private static WorldComparison toComparison(
            WorldComparisonResult comparison
    ) {
        return new WorldComparison(
                comparison.previousCount(),
                comparison.previousPercentage(),
                comparison.countChange(),
                comparison.countChangeRate(),
                comparison.percentageChangeRate(),
                comparison.percentagePointChange(),
                comparison.trend()
        );
    }

    public record WorldStatistics(
            String worldSlug,
            String worldName,
            long count,
            BigDecimal percentage,
            BigDecimal averageLevel,
            WorldComparison comparison
    ) {
    }

    public record WorldComparison(
            Long previousCount,
            BigDecimal previousPercentage,
            Long countChange,
            BigDecimal countChangeRate,
            BigDecimal percentageChangeRate,
            BigDecimal percentagePointChange,
            StatisticsTrend trend
    ) {
    }
}
