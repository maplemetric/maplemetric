package com.maplemetric.statistics.application.result;

import com.maplemetric.ranking.api.OverallRankingWorldStatisticsSnapshot;
import com.maplemetric.world.api.CanonicalWorld;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private static final BigDecimal ABSENT_PERCENTAGE =
            BigDecimal.ZERO.setScale(PERCENTAGE_SCALE);

    public static GetWorldStatisticsResult from(
            OverallRankingWorldStatisticsSnapshot snapshot,
            List<CanonicalWorld> canonicalWorlds,
            Map<String, CanonicalWorld> canonicalWorldsByWorldName
    ) {
        Map<String, CanonicalAggregate> aggregates = aggregateByWorldSlug(
                snapshot,
                canonicalWorldsByWorldName
        );

        List<WorldStatisticsResult> worlds = canonicalWorlds.stream()
                .map(canonicalWorld -> toWorldStatisticsResult(
                        canonicalWorld,
                        aggregates.get(canonicalWorld.worldSlug()),
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

    private static Map<String, CanonicalAggregate> aggregateByWorldSlug(
            OverallRankingWorldStatisticsSnapshot snapshot,
            Map<String, CanonicalWorld> canonicalWorldsByWorldName
    ) {
        Map<String, CanonicalAggregate> aggregates = new HashMap<>();

        snapshot.worldCounts().forEach(worldCount -> {
            CanonicalWorld canonicalWorld =
                    canonicalWorldsByWorldName.get(worldCount.worldName());

            if (canonicalWorld == null) {
                return;
            }

            aggregates.merge(
                    canonicalWorld.worldSlug(),
                    CanonicalAggregate.of(
                            worldCount.count(),
                            worldCount.averageLevel()
                    ),
                    (existing, added) -> existing.plus(added)
            );
        });

        return aggregates;
    }

    private static WorldStatisticsResult toWorldStatisticsResult(
            CanonicalWorld canonicalWorld,
            CanonicalAggregate aggregate,
            int sampleSize
    ) {
        long count = aggregate == null ? 0L : aggregate.count();

        return new WorldStatisticsResult(
                canonicalWorld.worldSlug(),
                canonicalWorld.worldName(),
                count,
                toPercentage(count, sampleSize),
                aggregate == null ? null : aggregate.averageLevel()
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
     * 같은 Canonical 월드에 매칭된 Alias들의 Count 합과 레벨 총합이다.
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

    public record WorldStatisticsResult(
            String worldSlug,
            String worldName,
            long count,
            BigDecimal percentage,
            BigDecimal averageLevel
    ) {
    }
}
