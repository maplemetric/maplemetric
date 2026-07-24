package com.maplemetric.ranking.application.service;

import com.maplemetric.ranking.api.OverallRankingWorldStatisticsQuery;
import com.maplemetric.ranking.api.OverallRankingWorldStatisticsQueryException;
import com.maplemetric.ranking.api.OverallRankingWorldStatisticsQueryFailure;
import com.maplemetric.ranking.api.OverallRankingWorldStatisticsSnapshot;
import com.maplemetric.ranking.api.OverallRankingWorldStatisticsSnapshot.WorldCount;
import com.maplemetric.ranking.application.port.out.LoadOverallRankingStatisticsPort;
import com.maplemetric.ranking.application.port.out.LoadOverallRankingStatisticsPort.LatestCollection;
import com.maplemetric.ranking.application.port.out.LoadOverallRankingStatisticsPort.WorldNameAggregate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OverallRankingWorldStatisticsQueryService
        implements OverallRankingWorldStatisticsQuery {

    private final LoadOverallRankingStatisticsPort loadOverallRankingStatisticsPort;

    OverallRankingWorldStatisticsQueryService(
            LoadOverallRankingStatisticsPort loadOverallRankingStatisticsPort
    ) {
        this.loadOverallRankingStatisticsPort =
                loadOverallRankingStatisticsPort;
    }

    @Override
    @Transactional(readOnly = true)
    public OverallRankingWorldStatisticsSnapshot getLatestWorldStatistics() {
        LatestCollection collection =
                loadOverallRankingStatisticsPort
                        .loadLatestAllConditionCollection()
                        .orElseThrow(() -> new OverallRankingWorldStatisticsQueryException(
                                OverallRankingWorldStatisticsQueryFailure.NOT_FOUND
                        ));

        List<WorldNameAggregate> aggregates =
                loadOverallRankingStatisticsPort.aggregateByWorldName(
                        collection.collectionId()
                );

        long totalCount = aggregates.stream()
                .mapToLong(aggregate -> aggregate.count())
                .sum();

        if (totalCount != collection.sampleSize()) {
            throw new OverallRankingWorldStatisticsQueryException(
                    OverallRankingWorldStatisticsQueryFailure.DATA_INVALID
            );
        }

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
}
