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

    /**
     * 보존된 전체 조건 Collection을 기준일 오름차순으로 모두 가져온다.
     *
     * 날짜 범위를 받지 않는다. 시작점은 DB에 남아 있는 최초 성공 Collection이며,
     * 호출자가 기간을 계산해 넘길 필요가 없다.
     */
    List<OverallRankingCollectionEntity> findAllConditionCollections();

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
