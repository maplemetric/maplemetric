package com.maplemetric.statistics.application.result;

import com.maplemetric.ranking.api.OverallRankingStatisticsSnapshot;
import com.maplemetric.ranking.api.OverallRankingStatisticsSnapshot.JobCount;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record GetJobStatisticsResult(
        List<JobStatisticsResult> jobs,
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

    public static GetJobStatisticsResult from(
            OverallRankingStatisticsSnapshot snapshot
    ) {
        List<JobStatisticsResult> jobs =
                snapshot.jobCounts()
                        .stream()
                        .map(jobCount -> toJobStatisticsResult(
                                jobCount,
                                snapshot.sampleSize()
                        ))
                        .toList();

        return new GetJobStatisticsResult(
                jobs,
                snapshot.sampleSize(),
                snapshot.asOf(),
                snapshot.source(),
                snapshot.collectedAt(),
                snapshot.pageCount(),
                snapshot.requestedMaxPages(),
                snapshot.truncated()
        );
    }

    private static JobStatisticsResult toJobStatisticsResult(
            JobCount jobCount,
            int sampleSize
    ) {
        BigDecimal percentage = BigDecimal.valueOf(jobCount.count())
                .multiply(BigDecimal.valueOf(100))
                .divide(
                        BigDecimal.valueOf(sampleSize),
                        PERCENTAGE_SCALE,
                        RoundingMode.HALF_UP
                );

        BigDecimal averageLevel = jobCount.averageLevel()
                .setScale(AVERAGE_LEVEL_SCALE, RoundingMode.HALF_UP);

        return new JobStatisticsResult(
                jobCount.className(),
                jobCount.count(),
                percentage,
                averageLevel
        );
    }

    public record JobStatisticsResult(
            String jobName,
            long count,
            BigDecimal percentage,
            BigDecimal averageLevel
    ) {
    }
}
