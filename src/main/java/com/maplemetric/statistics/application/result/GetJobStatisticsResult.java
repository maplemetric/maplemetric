package com.maplemetric.statistics.application.result;

import com.maplemetric.ranking.api.CanonicalJob;
import com.maplemetric.ranking.api.OverallRankingStatisticsComparisonSnapshot;
import com.maplemetric.ranking.api.OverallRankingStatisticsSnapshot;
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
            OverallRankingStatisticsComparisonSnapshot comparison,
            List<CanonicalJob> canonicalJobs,
            Map<String, CanonicalJob> canonicalJobsByClassName
    ) {
        OverallRankingStatisticsSnapshot latest = comparison.latest();
        OverallRankingStatisticsSnapshot previous = comparison.previous();

        boolean comparable = previous != null && previous.sampleSize() > 0;

        Map<String, CanonicalAggregate> latestAggregates = aggregateByJobSlug(
                latest,
                canonicalJobsByClassName
        );

        Map<String, BigDecimal> previousPercentages = comparable
                ? toPercentages(
                        aggregateByJobSlug(previous, canonicalJobsByClassName),
                        previous.sampleSize()
                )
                : Map.of();

        List<JobStatisticsResult> jobs = canonicalJobs.stream()
                .map(canonicalJob -> toJobStatisticsResult(
                        canonicalJob,
                        latestAggregates.get(canonicalJob.jobSlug()),
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

    private static Map<String, CanonicalAggregate> aggregateByJobSlug(
            OverallRankingStatisticsSnapshot snapshot,
            Map<String, CanonicalJob> canonicalJobsByClassName
    ) {
        Map<String, CanonicalAggregate> aggregates = new HashMap<>();

        snapshot.jobCounts().forEach(jobCount -> {
            CanonicalJob canonicalJob =
                    canonicalJobsByClassName.get(jobCount.className());

            if (canonicalJob == null) {
                return;
            }

            aggregates.merge(
                    canonicalJob.jobSlug(),
                    CanonicalAggregate.of(
                            jobCount.count(),
                            jobCount.averageLevel()
                    ),
                    (existing, added) -> existing.plus(added)
            );
        });

        return aggregates;
    }

    private static Map<String, BigDecimal> toPercentages(
            Map<String, CanonicalAggregate> aggregates,
            int sampleSize
    ) {
        Map<String, BigDecimal> percentages = new HashMap<>();

        aggregates.forEach((jobSlug, aggregate) -> percentages.put(
                jobSlug,
                toPercentage(aggregate.count(), sampleSize)
        ));

        return percentages;
    }

    private static JobStatisticsResult toJobStatisticsResult(
            CanonicalJob canonicalJob,
            CanonicalAggregate aggregate,
            int sampleSize,
            boolean comparable,
            Map<String, BigDecimal> previousPercentages
    ) {
        long count = aggregate == null ? 0L : aggregate.count();

        BigDecimal percentage = toPercentage(count, sampleSize);

        BigDecimal averageLevel = aggregate == null
                ? null
                : aggregate.averageLevel();

        BigDecimal changeRate = comparable
                ? percentage.subtract(previousPercentages.getOrDefault(
                        canonicalJob.jobSlug(),
                        ABSENT_PERCENTAGE
                ))
                : null;

        return new JobStatisticsResult(
                canonicalJob.jobSlug(),
                canonicalJob.jobName(),
                count,
                percentage,
                averageLevel,
                changeRate
        );
    }

    private static BigDecimal toPercentage(
            long count,
            int sampleSize
    ) {
        if (sampleSize <= 0) {
            return ABSENT_PERCENTAGE;
        }

        return BigDecimal.valueOf(count)
                .multiply(BigDecimal.valueOf(100))
                .divide(
                        BigDecimal.valueOf(sampleSize),
                        PERCENTAGE_SCALE,
                        RoundingMode.HALF_UP
                );
    }

    /**
     * 같은 Canonical 직업에 매칭된 Alias들의 Count 합과 레벨 총합이다.
     *
     * 레벨 총합은 Alias별 평균 레벨에 Count를 곱해 누적하므로,
     * 평균 레벨은 Alias 평균의 단순 평균이 아니라 Count 가중 평균이 된다.
     */
    private record CanonicalAggregate(
            long count,
            BigDecimal levelSum
    ) {

        private static CanonicalAggregate of(
                long count,
                BigDecimal averageLevel
        ) {
            return new CanonicalAggregate(
                    count,
                    averageLevel.multiply(BigDecimal.valueOf(count))
            );
        }

        private CanonicalAggregate plus(CanonicalAggregate added) {
            return new CanonicalAggregate(
                    count + added.count(),
                    levelSum.add(added.levelSum())
            );
        }

        private BigDecimal averageLevel() {
            if (count <= 0L) {
                return null;
            }

            return levelSum.divide(
                    BigDecimal.valueOf(count),
                    AVERAGE_LEVEL_SCALE,
                    RoundingMode.HALF_UP
            );
        }
    }

    public record JobStatisticsResult(
            String jobSlug,
            String jobName,
            long count,
            BigDecimal percentage,
            BigDecimal averageLevel,
            BigDecimal changeRate
    ) {
    }
}
