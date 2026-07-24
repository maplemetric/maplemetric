package com.maplemetric.ranking.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record OverallRankingWorldStatisticsSnapshot(
        LocalDate asOf,
        String source,
        Instant collectedAt,
        int sampleSize,
        int pageCount,
        int requestedMaxPages,
        boolean truncated,
        List<WorldCount> worldCounts
) {

    public record WorldCount(
            String worldName,
            long count,
            BigDecimal averageLevel
    ) {
    }
}
