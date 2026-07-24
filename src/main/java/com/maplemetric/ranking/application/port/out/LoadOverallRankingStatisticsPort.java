package com.maplemetric.ranking.application.port.out;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoadOverallRankingStatisticsPort {

    Optional<LatestCollection> loadLatestAllConditionCollection();

    List<ClassNameAggregate> aggregateByClassName(UUID collectionId);

    List<WorldNameAggregate> aggregateByWorldName(UUID collectionId);

    record LatestCollection(
            UUID collectionId,
            LocalDate snapshotDate,
            String source,
            Instant collectedAt,
            int sampleSize,
            int pageCount,
            int requestedMaxPages,
            boolean truncated
    ) {
    }

    record ClassNameAggregate(
            String className,
            long count,
            BigDecimal averageLevel
    ) {
    }

    record WorldNameAggregate(
            String worldName,
            long count,
            BigDecimal averageLevel
    ) {
    }
}
