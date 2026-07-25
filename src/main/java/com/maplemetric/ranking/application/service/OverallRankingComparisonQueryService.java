package com.maplemetric.ranking.application.service;

import com.maplemetric.ranking.api.OverallRankingComparisonQuery;
import com.maplemetric.ranking.api.OverallRankingComparisonQueryException;
import com.maplemetric.ranking.api.OverallRankingComparisonQueryFailure;
import com.maplemetric.ranking.api.OverallRankingStatisticsComparisonSnapshot;
import com.maplemetric.ranking.api.OverallRankingStatisticsSnapshot;
import com.maplemetric.ranking.api.OverallRankingStatisticsSnapshot.JobCount;
import com.maplemetric.ranking.api.OverallRankingWorldStatisticsComparisonSnapshot;
import com.maplemetric.ranking.api.OverallRankingWorldStatisticsSnapshot;
import com.maplemetric.ranking.api.OverallRankingWorldStatisticsSnapshot.WorldCount;
import com.maplemetric.ranking.application.port.out.LoadOverallRankingStatisticsPort;
import com.maplemetric.ranking.application.port.out.LoadOverallRankingStatisticsPort.ClassNameAggregate;
import com.maplemetric.ranking.application.port.out.LoadOverallRankingStatisticsPort.LatestCollection;
import com.maplemetric.ranking.application.port.out.LoadOverallRankingStatisticsPort.WorldNameAggregate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OverallRankingComparisonQueryService
        implements OverallRankingComparisonQuery {

    private final LoadOverallRankingStatisticsPort loadOverallRankingStatisticsPort;

    OverallRankingComparisonQueryService(
            LoadOverallRankingStatisticsPort loadOverallRankingStatisticsPort
    ) {
        this.loadOverallRankingStatisticsPort =
                loadOverallRankingStatisticsPort;
    }

    @Override
    @Transactional(readOnly = true)
    public OverallRankingStatisticsComparisonSnapshot getJobStatisticsComparison() {
        LatestCollection latest = loadLatestCollection();

        Optional<LatestCollection> previous =
                loadPreviousCollection(latest);

        if (previous.isEmpty()) {
            return new OverallRankingStatisticsComparisonSnapshot(
                    toJobStatisticsSnapshot(latest),
                    null,
                    null
            );
        }

        return new OverallRankingStatisticsComparisonSnapshot(
                toJobStatisticsSnapshot(latest),
                toJobStatisticsSnapshot(previous.get()),
                daysBetween(previous.get(), latest)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public OverallRankingWorldStatisticsComparisonSnapshot getWorldStatisticsComparison() {
        LatestCollection latest = loadLatestCollection();

        Optional<LatestCollection> previous =
                loadPreviousCollection(latest);

        if (previous.isEmpty()) {
            return new OverallRankingWorldStatisticsComparisonSnapshot(
                    toWorldStatisticsSnapshot(latest),
                    null,
                    null
            );
        }

        return new OverallRankingWorldStatisticsComparisonSnapshot(
                toWorldStatisticsSnapshot(latest),
                toWorldStatisticsSnapshot(previous.get()),
                daysBetween(previous.get(), latest)
        );
    }

    private LatestCollection loadLatestCollection() {
        return loadOverallRankingStatisticsPort
                .loadLatestAllConditionCollection()
                .orElseThrow(() -> new OverallRankingComparisonQueryException(
                        OverallRankingComparisonQueryFailure.NOT_FOUND
                ));
    }

    private Optional<LatestCollection> loadPreviousCollection(
            LatestCollection latest
    ) {
        Optional<LatestCollection> previous =
                loadOverallRankingStatisticsPort
                        .loadPreviousAllConditionCollection(
                                latest.snapshotDate()
                        );

        previous.ifPresent(collection -> {
            if (!collection.snapshotDate()
                    .isBefore(latest.snapshotDate())) {
                throw new OverallRankingComparisonQueryException(
                        OverallRankingComparisonQueryFailure.DATA_INVALID
                );
            }
        });

        return previous;
    }

    private OverallRankingStatisticsSnapshot toJobStatisticsSnapshot(
            LatestCollection collection
    ) {
        List<ClassNameAggregate> aggregates =
                loadOverallRankingStatisticsPort.aggregateByClassName(
                        collection.collectionId()
                );

        long totalCount = aggregates.stream()
                .mapToLong(aggregate -> aggregate.count())
                .sum();

        requireSampleSizeMatched(totalCount, collection.sampleSize());

        List<JobCount> jobCounts = aggregates.stream()
                .map(aggregate -> new JobCount(
                        aggregate.className(),
                        aggregate.count(),
                        aggregate.averageLevel()
                ))
                .toList();

        return new OverallRankingStatisticsSnapshot(
                collection.snapshotDate(),
                collection.source(),
                collection.collectedAt(),
                collection.sampleSize(),
                collection.pageCount(),
                collection.requestedMaxPages(),
                collection.truncated(),
                jobCounts
        );
    }

    private OverallRankingWorldStatisticsSnapshot toWorldStatisticsSnapshot(
            LatestCollection collection
    ) {
        List<WorldNameAggregate> aggregates =
                loadOverallRankingStatisticsPort.aggregateByWorldName(
                        collection.collectionId()
                );

        long totalCount = aggregates.stream()
                .mapToLong(aggregate -> aggregate.count())
                .sum();

        requireSampleSizeMatched(totalCount, collection.sampleSize());

        List<WorldCount> worldCounts = aggregates.stream()
                .map(aggregate -> new WorldCount(
                        aggregate.worldName(),
                        aggregate.count(),
                        aggregate.averageLevel()
                ))
                .toList();

        return new OverallRankingWorldStatisticsSnapshot(
                collection.snapshotDate(),
                collection.source(),
                collection.collectedAt(),
                collection.sampleSize(),
                collection.pageCount(),
                collection.requestedMaxPages(),
                collection.truncated(),
                worldCounts
        );
    }

    private void requireSampleSizeMatched(
            long totalCount,
            int sampleSize
    ) {
        if (totalCount != sampleSize) {
            throw new OverallRankingComparisonQueryException(
                    OverallRankingComparisonQueryFailure.DATA_INVALID
            );
        }
    }

    private int daysBetween(
            LatestCollection previous,
            LatestCollection latest
    ) {
        return (int) ChronoUnit.DAYS.between(
                previous.snapshotDate(),
                latest.snapshotDate()
        );
    }
}
