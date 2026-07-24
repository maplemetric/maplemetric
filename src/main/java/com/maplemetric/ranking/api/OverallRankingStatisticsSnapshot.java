package com.maplemetric.ranking.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record OverallRankingStatisticsSnapshot(
        LocalDate asOf,
        String source,
        Instant collectedAt,
        int sampleSize,
        int pageCount,
        int requestedMaxPages,
        boolean truncated,
        List<JobCount> jobCounts
) {

    public record JobCount(
            String className,
            long count,
            BigDecimal averageLevel
    ) {
    }
}
