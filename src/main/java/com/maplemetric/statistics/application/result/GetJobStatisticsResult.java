package com.maplemetric.statistics.application.result;

import com.maplemetric.ranking.api.OverallRankingStatisticsComparisonSnapshot;
import com.maplemetric.ranking.api.OverallRankingStatisticsSnapshot;
import com.maplemetric.ranking.api.OverallRankingStatisticsSnapshot.JobCount;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record GetJobStatisticsResult(
        List<JobStatisticsResult> jobs,
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

    private static final BigDecimal ABSENT_PERCENTAGE =
            BigDecimal.ZERO.setScale(PERCENTAGE_SCALE);

    public static GetJobStatisticsResult from(
            OverallRankingStatisticsComparisonSnapshot comparison
    ) {
        OverallRankingStatisticsSnapshot latest = comparison.latest();
        OverallRankingStatisticsSnapshot previous = comparison.previous();

        boolean comparable = previous != null && previous.sampleSize() > 0;

        Map<String, BigDecimal> previousPercentages = comparable
                ? toPercentages(previous)
                : Map.of();

        List<JobStatisticsResult> jobs =
                latest.jobCounts()
                        .stream()
                        .map(jobCount -> toJobStatisticsResult(
                                jobCount,
                                latest.sampleSize(),
                                comparable,
                                previousPercentages
                        ))
                        .toList();

        return new GetJobStatisticsResult(
                jobs,
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

    private static Map<String, BigDecimal> toPercentages(
            OverallRankingStatisticsSnapshot snapshot
    ) {
        Map<String, BigDecimal> percentages = new HashMap<>();

        snapshot.jobCounts()
                .forEach(jobCount -> percentages.put(
                        jobCount.className(),
                        toPercentage(
                                jobCount.count(),
                                snapshot.sampleSize()
                        )
                ));

        return percentages;
    }

    private static JobStatisticsResult toJobStatisticsResult(
            JobCount jobCount,
            int sampleSize,
            boolean comparable,
            Map<String, BigDecimal> previousPercentages
    ) {
        BigDecimal percentage = toPercentage(
                jobCount.count(),
                sampleSize
        );

        BigDecimal averageLevel = jobCount.averageLevel()
                .setScale(AVERAGE_LEVEL_SCALE, RoundingMode.HALF_UP);

        BigDecimal changeRate = comparable
                ? percentage.subtract(previousPercentages.getOrDefault(
                        jobCount.className(),
                        ABSENT_PERCENTAGE
                ))
                : null;

        return new JobStatisticsResult(
                jobCount.className(),
                jobCount.count(),
                percentage,
                averageLevel,
                changeRate
        );
    }

    private static BigDecimal toPercentage(
            long count,
            int sampleSize
    ) {
        return BigDecimal.valueOf(count)
                .multiply(BigDecimal.valueOf(100))
                .divide(
                        BigDecimal.valueOf(sampleSize),
                        PERCENTAGE_SCALE,
                        RoundingMode.HALF_UP
                );
    }

    public record JobStatisticsResult(
            String jobName,
            long count,
            BigDecimal percentage,
            BigDecimal averageLevel,
            BigDecimal changeRate
    ) {
    }
}
