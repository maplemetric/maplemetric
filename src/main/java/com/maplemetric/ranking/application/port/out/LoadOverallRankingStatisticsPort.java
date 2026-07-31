package com.maplemetric.ranking.application.port.out;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoadOverallRankingStatisticsPort {

    Optional<LatestCollection> loadLatestAllConditionCollection();

    Optional<LatestCollection> loadPreviousAllConditionCollection(
            LocalDate baseSnapshotDate
    );

    List<LatestCollection> loadAllConditionCollectionsBetween(
            LocalDate from,
            LocalDate to
    );

    List<ClassNameAggregate> aggregateByClassName(UUID collectionId);

    List<ClassNameAggregateByCollection> aggregateByClassName(
            List<UUID> collectionIds
    );

    List<WorldNameAggregate> aggregateByWorldName(UUID collectionId);

    List<WorldNameAggregateByCollection> aggregateByWorldName(
            List<UUID> collectionIds
    );

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

    record ClassNameAggregateByCollection(
            UUID collectionId,
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

    record WorldNameAggregateByCollection(
            UUID collectionId,
            String worldName,
            long count,
            BigDecimal averageLevel
    ) {
    }
}
