package com.maplemetric.ranking.infrastructure.persistence.querydsl;

import com.maplemetric.ranking.infrastructure.persistence.OverallRankingCollectionEntity;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OverallRankingStatisticsQueryDslRepository {

    Optional<OverallRankingCollectionEntity> findLatestAllConditionCollection();

    List<ClassNameAggregate> aggregateByClassName(UUID collectionId);

    record ClassNameAggregate(
            String className,
            long count,
            BigDecimal averageLevel
    ) {
    }
}
