package com.maplemetric.statistics.application.result;

import com.maplemetric.ranking.api.OverallRankingWorldStatisticsSnapshot;
import com.maplemetric.ranking.api.OverallRankingWorldStatisticsSnapshot.WorldCount;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record GetWorldStatisticsResult(
        List<WorldStatisticsResult> worlds,
        int sampleSize,
        LocalDate asOf,
        String source,
        Instant collectedAt,
        int pageCount,
        int requestedMaxPages,
        boolean truncated
) {

    private static final int PERCENTAGE_SCALE = 2;
    private static final int AVERAGE_LEVEL_SCALE = 1;

    public static GetWorldStatisticsResult from(
            OverallRankingWorldStatisticsSnapshot snapshot
    ) {
        List<WorldStatisticsResult> worlds =
                snapshot.worldCounts()
                        .stream()
                        .map(worldCount -> toWorldStatisticsResult(
                                worldCount,
                                snapshot.sampleSize()
                        ))
                        .toList();

        return new GetWorldStatisticsResult(
                worlds,
                snapshot.sampleSize(),
                snapshot.asOf(),
                snapshot.source(),
                snapshot.collectedAt(),
                snapshot.pageCount(),
                snapshot.requestedMaxPages(),
                snapshot.truncated()
        );
    }

    private static WorldStatisticsResult toWorldStatisticsResult(
            WorldCount worldCount,
            int sampleSize
    ) {
        BigDecimal percentage = BigDecimal.valueOf(worldCount.count())
                .multiply(BigDecimal.valueOf(100))
                .divide(
                        BigDecimal.valueOf(sampleSize),
                        PERCENTAGE_SCALE,
                        RoundingMode.HALF_UP
                );

        BigDecimal averageLevel = worldCount.averageLevel()
                .setScale(AVERAGE_LEVEL_SCALE, RoundingMode.HALF_UP);

        return new WorldStatisticsResult(
                worldCount.worldName(),
                worldCount.count(),
                percentage,
                averageLevel
        );
    }

    public record WorldStatisticsResult(
            String worldName,
            long count,
            BigDecimal percentage,
            BigDecimal averageLevel
    ) {
    }
}
