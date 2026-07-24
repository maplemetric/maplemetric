package com.maplemetric.statistics.presentation.response;

import com.maplemetric.statistics.application.result.GetWorldStatisticsResult;
import com.maplemetric.statistics.application.result.GetWorldStatisticsResult.WorldStatisticsResult;
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
        boolean truncated
) {

    public static GetWorldStatisticsResponse from(
            GetWorldStatisticsResult result
    ) {
        List<WorldStatistics> worlds = result.worlds()
                .stream()
                .map(world -> new WorldStatistics(
                        world.worldName(),
                        world.count(),
                        world.percentage(),
                        world.averageLevel()
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
                result.truncated()
        );
    }

    public record WorldStatistics(
            String worldName,
            long count,
            BigDecimal percentage,
            BigDecimal averageLevel
    ) {
    }
}
