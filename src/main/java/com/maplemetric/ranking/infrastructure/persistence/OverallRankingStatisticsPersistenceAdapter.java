package com.maplemetric.ranking.infrastructure.persistence;

import com.maplemetric.ranking.application.port.out.LoadOverallRankingStatisticsPort;
import com.maplemetric.ranking.infrastructure.persistence.querydsl.OverallRankingStatisticsQueryDslRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class OverallRankingStatisticsPersistenceAdapter
        implements LoadOverallRankingStatisticsPort {

    private final OverallRankingStatisticsQueryDslRepository queryDslRepository;

    OverallRankingStatisticsPersistenceAdapter(
            OverallRankingStatisticsQueryDslRepository queryDslRepository
    ) {
        this.queryDslRepository = queryDslRepository;
    }

    @Override
    public Optional<LatestCollection> loadLatestAllConditionCollection() {
        return queryDslRepository
                .findLatestAllConditionCollection()
                .map(collection -> toLatestCollection(collection));
    }

    @Override
    public Optional<LatestCollection> loadPreviousAllConditionCollection(
            LocalDate baseSnapshotDate
    ) {
        return queryDslRepository
                .findPreviousAllConditionCollection(baseSnapshotDate)
                .map(collection -> toLatestCollection(collection));
    }

    @Override
    public List<LatestCollection> loadAllConditionCollectionsWithin(
            LocalDate baseSnapshotDate,
            int days
    ) {
        return queryDslRepository
                .findAllConditionCollectionsWithin(baseSnapshotDate, days)
                .stream()
                .map(collection -> toLatestCollection(collection))
                .toList();
    }

    private LatestCollection toLatestCollection(
            OverallRankingCollectionEntity collection
    ) {
        return new LatestCollection(
                collection.getId(),
                collection.getSnapshotDate(),
                collection.getSource(),
                collection.getCollectedAt(),
                collection.getSampleSize(),
                collection.getPageCount(),
                collection.getRequestedMaxPages(),
                collection.isTruncated()
        );
    }

    @Override
    public List<ClassNameAggregate> aggregateByClassName(
            UUID collectionId
    ) {
        return queryDslRepository
                .aggregateByClassName(collectionId)
                .stream()
                .map(aggregate -> new ClassNameAggregate(
                        aggregate.className(),
                        aggregate.count(),
                        aggregate.averageLevel()
                ))
                .toList();
    }

    @Override
    public List<ClassNameAggregateByCollection> aggregateByClassName(
            List<UUID> collectionIds
    ) {
        return queryDslRepository
                .aggregateByClassName(collectionIds)
                .stream()
                .map(aggregate -> new ClassNameAggregateByCollection(
                        aggregate.collectionId(),
                        aggregate.className(),
                        aggregate.count(),
                        aggregate.averageLevel()
                ))
                .toList();
    }

    @Override
    public List<WorldNameAggregate> aggregateByWorldName(
            UUID collectionId
    ) {
        return queryDslRepository
                .aggregateByWorldName(collectionId)
                .stream()
                .map(aggregate -> new WorldNameAggregate(
                        aggregate.worldName(),
                        aggregate.count(),
                        aggregate.averageLevel()
                ))
                .toList();
    }
}
