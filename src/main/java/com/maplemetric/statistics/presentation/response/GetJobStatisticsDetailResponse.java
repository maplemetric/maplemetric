package com.maplemetric.statistics.presentation.response;

import com.maplemetric.statistics.application.result.GetJobStatisticsDetailResult;
import com.maplemetric.statistics.api.StatisticsDataAvailability;
import com.maplemetric.statistics.api.StatisticsTrend;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record GetJobStatisticsDetailResponse(
        Job job,
        StatisticsDataAvailability dataAvailability,
        Latest latest,
        Comparison comparison,
        SourceMeta sourceMeta
) {

    public static GetJobStatisticsDetailResponse from(
            GetJobStatisticsDetailResult result
    ) {
        return new GetJobStatisticsDetailResponse(
                new Job(
                        result.job().jobSlug(),
                        result.job().jobName(),
                        result.job().jobGroup(),
                        result.job().jobBranch(),
                        result.job().available()
                ),
                result.dataAvailability(),
                result.latest() == null
                        ? null
                        : new Latest(
                                result.latest().asOf(),
                                result.latest().count(),
                                result.latest().percentage(),
                                result.latest().averageLevel()
                        ),
                result.comparison() == null
                        ? null
                        : new Comparison(
                                result.comparison().previousAsOf(),
                                result.comparison().previousCount(),
                                result.comparison().countChange(),
                                result.comparison().countChangeRate(),
                                result.comparison().previousPercentage(),
                                result.comparison().percentageChangeRate(),
                                result.comparison()
                                        .percentagePointChange(),
                                result.comparison().trend()
                        ),
                result.sourceMeta() == null
                        ? null
                        : new SourceMeta(
                                result.sourceMeta().source(),
                                result.sourceMeta().collectedAt(),
                                result.sourceMeta().sampleSize(),
                                result.sourceMeta().pageCount(),
                                result.sourceMeta().requestedMaxPages(),
                                result.sourceMeta().truncated()
                        )
        );
    }

    public record Job(
            String jobSlug,
            String jobName,
            String jobGroup,
            String jobBranch,
            boolean available
    ) {
    }

    public record Latest(
            LocalDate asOf,
            long count,
            BigDecimal percentage,
            BigDecimal averageLevel
    ) {
    }

    public record Comparison(
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

    public record SourceMeta(
            String source,
            Instant collectedAt,
            int sampleSize,
            int pageCount,
            int requestedMaxPages,
            boolean truncated
    ) {
    }
}
