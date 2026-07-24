package com.maplemetric.ranking.infrastructure.persistence.querydsl;

import com.maplemetric.ranking.infrastructure.persistence.OverallRankingCollectionEntity;
import com.maplemetric.ranking.infrastructure.persistence.QOverallRankingCollectionEntity;
import com.maplemetric.ranking.infrastructure.persistence.QOverallRankingSnapshotEntity;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
class OverallRankingStatisticsQueryDslRepositoryImpl
        implements OverallRankingStatisticsQueryDslRepository {

    private final JPAQueryFactory queryFactory;

    OverallRankingStatisticsQueryDslRepositoryImpl(
            JPAQueryFactory queryFactory
    ) {
        this.queryFactory = queryFactory;
    }

    @Override
    public Optional<OverallRankingCollectionEntity> findLatestAllConditionCollection() {
        QOverallRankingCollectionEntity collection =
                QOverallRankingCollectionEntity.overallRankingCollectionEntity;

        OverallRankingCollectionEntity result = queryFactory
                .selectFrom(collection)
                .where(
                        collection.worldName.eq(
                                OverallRankingCollectionEntity.ALL_WORLD_NAME
                        ),
                        collection.worldType.eq(
                                OverallRankingCollectionEntity.ALL_WORLD_TYPE
                        ),
                        collection.className.eq(
                                OverallRankingCollectionEntity.ALL_CLASS_NAME
                        )
                )
                .orderBy(
                        collection.snapshotDate.desc(),
                        collection.collectedAt.desc()
                )
                .fetchFirst();

        return Optional.ofNullable(result);
    }

    @Override
    public List<ClassNameAggregate> aggregateByClassName(
            UUID collectionId
    ) {
        QOverallRankingSnapshotEntity snapshot =
                QOverallRankingSnapshotEntity.overallRankingSnapshotEntity;

        NumberExpression<BigDecimal> averageLevel =
                Expressions.numberTemplate(
                        BigDecimal.class,
                        "avg({0})",
                        snapshot.characterLevel
                );

        NumberExpression<Long> count = snapshot.count();

        return queryFactory
                .select(Projections.constructor(
                        ClassNameAggregate.class,
                        snapshot.className,
                        count,
                        averageLevel
                ))
                .from(snapshot)
                .where(snapshot.collection.id.eq(collectionId))
                .groupBy(snapshot.className)
                .orderBy(count.desc(), snapshot.className.asc())
                .fetch();
    }
}
