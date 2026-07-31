package com.maplemetric.ranking.infrastructure.persistence.querydsl;

import com.maplemetric.ranking.infrastructure.persistence.OverallRankingCollectionEntity;
import com.maplemetric.ranking.infrastructure.persistence.QOverallRankingCollectionEntity;
import com.maplemetric.ranking.infrastructure.persistence.QOverallRankingSnapshotEntity;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.StringExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.math.BigDecimal;
import java.time.LocalDate;
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
                .where(allCondition(collection))
                .orderBy(
                        collection.snapshotDate.desc(),
                        collection.collectedAt.desc()
                )
                .fetchFirst();

        return Optional.ofNullable(result);
    }

    @Override
    public Optional<OverallRankingCollectionEntity> findPreviousAllConditionCollection(
            LocalDate baseSnapshotDate
    ) {
        QOverallRankingCollectionEntity collection =
                QOverallRankingCollectionEntity.overallRankingCollectionEntity;

        OverallRankingCollectionEntity result = queryFactory
                .selectFrom(collection)
                .where(
                        allCondition(collection),
                        collection.snapshotDate.lt(baseSnapshotDate)
                )
                .orderBy(
                        collection.snapshotDate.desc(),
                        collection.collectedAt.desc()
                )
                .fetchFirst();

        return Optional.ofNullable(result);
    }

    @Override
    public List<OverallRankingCollectionEntity> findAllConditionCollectionsWithin(
            LocalDate baseSnapshotDate,
            int days
    ) {
        QOverallRankingCollectionEntity collection =
                QOverallRankingCollectionEntity.overallRankingCollectionEntity;

        return queryFactory
                .selectFrom(collection)
                .where(
                        allCondition(collection),
                        collection.snapshotDate.loe(baseSnapshotDate),
                        collection.snapshotDate.gt(
                                baseSnapshotDate.minusDays(days)
                        )
                )
                .orderBy(collection.snapshotDate.asc())
                .fetch();
    }

    private BooleanExpression allCondition(
            QOverallRankingCollectionEntity collection
    ) {
        return collection.worldName
                .eq(OverallRankingCollectionEntity.ALL_WORLD_NAME)
                .and(collection.worldType.eq(
                        OverallRankingCollectionEntity.ALL_WORLD_TYPE
                ))
                .and(collection.className.eq(
                        OverallRankingCollectionEntity.ALL_CLASS_NAME
                ));
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
        StringExpression jobName = jobName(snapshot);

        return queryFactory
                .select(Projections.constructor(
                        ClassNameAggregate.class,
                        jobName,
                        count,
                        averageLevel
                ))
                .from(snapshot)
                .where(snapshot.collection.id.eq(collectionId))
                .groupBy(jobName)
                .orderBy(count.desc(), jobName.asc())
                .fetch();
    }

    @Override
    public List<ClassNameAggregateByCollection> aggregateByClassName(
            List<UUID> collectionIds
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
        StringExpression jobName = jobName(snapshot);

        return queryFactory
                .select(Projections.constructor(
                        ClassNameAggregateByCollection.class,
                        snapshot.collection.id,
                        jobName,
                        count,
                        averageLevel
                ))
                .from(snapshot)
                .where(snapshot.collection.id.in(collectionIds))
                .groupBy(snapshot.collection.id, jobName)
                .orderBy(
                        snapshot.collection.id.asc(),
                        count.desc(),
                        jobName.asc()
                )
                .fetch();
    }

    private StringExpression jobName(
            QOverallRankingSnapshotEntity snapshot
    ) {
        return Expressions.stringTemplate(
                "case when nullif(function('regexp_replace', {0}, "
                        + "'[[:space:]]', '', 'g'), '') is not null "
                        + "then {0} else {1} end",
                snapshot.subClassName,
                snapshot.className
        );
    }

    @Override
    public List<WorldNameAggregate> aggregateByWorldName(
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
                        WorldNameAggregate.class,
                        snapshot.worldName,
                        count,
                        averageLevel
                ))
                .from(snapshot)
                .where(snapshot.collection.id.eq(collectionId))
                .groupBy(snapshot.worldName)
                .orderBy(count.desc(), snapshot.worldName.asc())
                .fetch();
    }
}
