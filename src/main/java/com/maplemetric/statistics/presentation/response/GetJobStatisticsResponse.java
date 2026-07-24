package com.maplemetric.statistics.presentation.response;

import com.maplemetric.statistics.application.result.GetJobStatisticsResult;
import com.maplemetric.statistics.application.result.GetJobStatisticsResult.JobStatisticsResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record GetJobStatisticsResponse(
        List<JobStatistics> jobs,
        int sampleSize,
        LocalDate asOf,
        String source,
        Instant collectedAt,
        int pageCount,
        int requestedMaxPages,
        boolean truncated
) {

    public static GetJobStatisticsResponse from(
            GetJobStatisticsResult result
    ) {
        List<JobStatistics> jobs = result.jobs()
                .stream()
                .map(job -> new JobStatistics(
                        job.jobName(),
                        job.count(),
                        job.percentage(),
                        job.averageLevel()
                ))
                .toList();

        return new GetJobStatisticsResponse(
                jobs,
                result.sampleSize(),
                result.asOf(),
                result.source(),
                result.collectedAt(),
                result.pageCount(),
                result.requestedMaxPages(),
                result.truncated()
        );
    }

    public record JobStatistics(
            String jobName,
            long count,
            BigDecimal percentage,
            BigDecimal averageLevel
    ) {
    }
}
