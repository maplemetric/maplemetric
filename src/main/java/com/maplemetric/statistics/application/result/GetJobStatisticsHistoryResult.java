package com.maplemetric.statistics.application.result;

import com.maplemetric.ranking.api.CanonicalJob;
import com.maplemetric.ranking.api.OverallRankingStatisticsComparisonSnapshot;
import com.maplemetric.ranking.api.OverallRankingStatisticsSnapshot;
import com.maplemetric.statistics.application.result.GetJobStatisticsResult.JobComparisonResult;
import com.maplemetric.statistics.application.result.GetJobStatisticsResult.JobStatisticsResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

public record GetJobStatisticsHistoryResult(
        JobResult job,
        StatisticsDataAvailability dataAvailability,
        RangeResult range,
        RangeComparisonResult rangeComparison,
        List<PointResult> points,
        List<String> limitations
) {

    public static GetJobStatisticsHistoryResult available(
            CanonicalJob canonicalJob,
            String preset,
            LocalDate requestedFrom,
            LocalDate requestedTo,
            List<OverallRankingStatisticsSnapshot> snapshots,
            Map<String, CanonicalJob> canonicalJobsByClassName
    ) {
        List<PointResult> points = snapshots.stream()
                .map(snapshot -> toPoint(
                        canonicalJob,
                        snapshot,
                        canonicalJobsByClassName
                ))
                .toList();

        LocalDate firstAsOf = points.isEmpty()
                ? null
                : points.get(0).asOf();

        LocalDate lastAsOf = points.isEmpty()
                ? null
                : points.get(points.size() - 1).asOf();

        int requestedDateCount = Math.toIntExact(
                ChronoUnit.DAYS.between(requestedFrom, requestedTo) + 1
        );

        return new GetJobStatisticsHistoryResult(
                JobResult.from(canonicalJob),
                StatisticsDataAvailability.AVAILABLE,
                new RangeResult(
                        preset,
                        requestedFrom,
                        requestedTo,
                        firstAsOf,
                        lastAsOf,
                        points.size(),
                        requestedDateCount - points.size()
                ),
                toRangeComparison(
                        canonicalJob,
                        snapshots,
                        canonicalJobsByClassName
                ),
                points,
                List.of()
        );
    }

    public static GetJobStatisticsHistoryResult notCollected(
            CanonicalJob canonicalJob,
            String preset,
            LocalDate requestedFrom,
            LocalDate requestedTo
    ) {
        int missingDateCount =
                requestedFrom == null || requestedTo == null
                        ? 0
                        : Math.toIntExact(
                                ChronoUnit.DAYS.between(
                                        requestedFrom,
                                        requestedTo
                                ) + 1
                        );

        return new GetJobStatisticsHistoryResult(
                JobResult.from(canonicalJob),
                StatisticsDataAvailability.NOT_COLLECTED,
                new RangeResult(
                        preset,
                        requestedFrom,
                        requestedTo,
                        null,
                        null,
                        0,
                        missingDateCount
                ),
                null,
                List.of(),
                List.of()
        );
    }

    private static PointResult toPoint(
            CanonicalJob canonicalJob,
            OverallRankingStatisticsSnapshot snapshot,
            Map<String, CanonicalJob> canonicalJobsByClassName
    ) {
        JobStatisticsResult statistics = calculate(
                canonicalJob,
                new OverallRankingStatisticsComparisonSnapshot(
                        snapshot,
                        null,
                        null
                ),
                canonicalJobsByClassName
        );

        return new PointResult(
                snapshot.asOf(),
                statistics.count(),
                statistics.percentage(),
                statistics.averageLevel(),
                snapshot.sampleSize(),
                snapshot.pageCount(),
                snapshot.requestedMaxPages(),
                snapshot.truncated(),
                snapshot.collectedAt()
        );
    }

    private static RangeComparisonResult toRangeComparison(
            CanonicalJob canonicalJob,
            List<OverallRankingStatisticsSnapshot> snapshots,
            Map<String, CanonicalJob> canonicalJobsByClassName
    ) {
        if (snapshots.size() < 2) {
            return null;
        }

        OverallRankingStatisticsSnapshot first = snapshots.get(0);
        OverallRankingStatisticsSnapshot last =
                snapshots.get(snapshots.size() - 1);

        int daysBetween = Math.toIntExact(
                ChronoUnit.DAYS.between(first.asOf(), last.asOf())
        );

        JobStatisticsResult current = calculate(
                canonicalJob,
                new OverallRankingStatisticsComparisonSnapshot(
                        last,
                        first,
                        daysBetween
                ),
                canonicalJobsByClassName
        );

        JobComparisonResult comparison = current.comparison();

        return new RangeComparisonResult(
                first.asOf(),
                last.asOf(),
                comparison.previousCount(),
                current.count(),
                comparison.countChange(),
                comparison.countChangeRate(),
                comparison.previousPercentage(),
                current.percentage(),
                comparison.percentageChangeRate(),
                comparison.percentagePointChange()
        );
    }

    private static JobStatisticsResult calculate(
            CanonicalJob canonicalJob,
            OverallRankingStatisticsComparisonSnapshot comparison,
            Map<String, CanonicalJob> canonicalJobsByClassName
    ) {
        return GetJobStatisticsResult.from(
                comparison,
                List.of(canonicalJob),
                canonicalJobsByClassName
        ).jobs().get(0);
    }

    public record JobResult(
            String jobSlug,
            String jobName
    ) {

        private static JobResult from(CanonicalJob canonicalJob) {
            return new JobResult(
                    canonicalJob.jobSlug(),
                    canonicalJob.jobName()
            );
        }
    }

    public record RangeResult(
            String preset,
            LocalDate requestedFrom,
            LocalDate requestedTo,
            LocalDate firstAsOf,
            LocalDate lastAsOf,
            int pointCount,
            int missingDateCount
    ) {
    }

    public record RangeComparisonResult(
            LocalDate fromAsOf,
            LocalDate toAsOf,
            Long previousCount,
            long currentCount,
            Long countChange,
            BigDecimal countChangeRate,
            BigDecimal previousPercentage,
            BigDecimal currentPercentage,
            BigDecimal percentageChangeRate,
            BigDecimal percentagePointChange
    ) {
    }

    public record PointResult(
            LocalDate asOf,
            long count,
            BigDecimal percentage,
            BigDecimal averageLevel,
            int sampleSize,
            int pageCount,
            int requestedMaxPages,
            boolean truncated,
            Instant collectedAt
    ) {
    }
}
