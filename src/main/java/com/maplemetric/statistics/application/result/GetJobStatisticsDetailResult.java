package com.maplemetric.statistics.application.result;

import com.maplemetric.ranking.api.CanonicalJob;
import com.maplemetric.ranking.api.OverallRankingStatisticsComparisonSnapshot;
import com.maplemetric.statistics.api.StatisticsDataAvailability;
import com.maplemetric.statistics.api.StatisticsTrend;
import com.maplemetric.statistics.application.result.GetJobStatisticsResult.JobComparisonResult;
import com.maplemetric.statistics.application.result.GetJobStatisticsResult.JobStatisticsResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record GetJobStatisticsDetailResult(
        JobResult job,
        StatisticsDataAvailability dataAvailability,
        LatestResult latest,
        ComparisonResult comparison,
        SourceMetaResult sourceMeta
) {

    public static GetJobStatisticsDetailResult available(
            CanonicalJob canonicalJob,
            OverallRankingStatisticsComparisonSnapshot rankingComparison,
            Map<String, CanonicalJob> canonicalJobsByClassName
    ) {
        GetJobStatisticsResult statistics =
                GetJobStatisticsResult.from(
                        rankingComparison,
                        List.of(canonicalJob),
                        canonicalJobsByClassName
                );

        JobStatisticsResult jobStatistics = statistics.jobs().get(0);
        JobComparisonResult jobComparison = jobStatistics.comparison();

        return new GetJobStatisticsDetailResult(
                JobResult.from(canonicalJob),
                StatisticsDataAvailability.AVAILABLE,
                new LatestResult(
                        statistics.asOf(),
                        jobStatistics.count(),
                        jobStatistics.percentage(),
                        jobStatistics.averageLevel()
                ),
                new ComparisonResult(
                        statistics.previousAsOf(),
                        jobComparison.previousCount(),
                        jobComparison.countChange(),
                        jobComparison.countChangeRate(),
                        jobComparison.previousPercentage(),
                        jobComparison.percentageChangeRate(),
                        jobComparison.percentagePointChange(),
                        jobComparison.trend()
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

    public static GetJobStatisticsDetailResult notCollected(
            CanonicalJob canonicalJob
    ) {
        return new GetJobStatisticsDetailResult(
                JobResult.from(canonicalJob),
                StatisticsDataAvailability.NOT_COLLECTED,
                null,
                null,
                null
        );
    }

    public record JobResult(
            String jobSlug,
            String jobName,
            String jobGroup,
            String jobBranch,
            boolean available
    ) {

        private static JobResult from(CanonicalJob canonicalJob) {
            return new JobResult(
                    canonicalJob.jobSlug(),
                    canonicalJob.jobName(),
                    canonicalJob.jobGroup(),
                    canonicalJob.jobBranch(),
                    canonicalJob.available()
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
