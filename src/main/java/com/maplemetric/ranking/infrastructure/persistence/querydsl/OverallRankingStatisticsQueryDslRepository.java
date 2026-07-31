package com.maplemetric.ranking.infrastructure.persistence.querydsl;

import com.maplemetric.ranking.infrastructure.persistence.OverallRankingCollectionEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OverallRankingStatisticsQueryDslRepository {

    Optional<OverallRankingCollectionEntity> findLatestAllConditionCollection();

    Optional<OverallRankingCollectionEntity> findPreviousAllConditionCollection(
            LocalDate baseSnapshotDate
    );

    List<OverallRankingCollectionEntity> findAllConditionCollectionsBetween(
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
