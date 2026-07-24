package com.maplemetric.ranking.application.service;

import com.maplemetric.ranking.api.OverallRankingStatisticsQuery;
import com.maplemetric.ranking.api.OverallRankingStatisticsQueryException;
import com.maplemetric.ranking.api.OverallRankingStatisticsQueryFailure;
import com.maplemetric.ranking.api.OverallRankingStatisticsSnapshot;
import com.maplemetric.ranking.api.OverallRankingStatisticsSnapshot.JobCount;
import com.maplemetric.ranking.application.port.out.LoadOverallRankingStatisticsPort;
import com.maplemetric.ranking.application.port.out.LoadOverallRankingStatisticsPort.ClassNameAggregate;
import com.maplemetric.ranking.application.port.out.LoadOverallRankingStatisticsPort.LatestCollection;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OverallRankingStatisticsQueryService
        implements OverallRankingStatisticsQuery {

    private final LoadOverallRankingStatisticsPort loadOverallRankingStatisticsPort;

    OverallRankingStatisticsQueryService(
            LoadOverallRankingStatisticsPort loadOverallRankingStatisticsPort
    ) {
        this.loadOverallRankingStatisticsPort =
                loadOverallRankingStatisticsPort;
    }

    @Override
    @Transactional(readOnly = true)
    public OverallRankingStatisticsSnapshot getLatestJobStatistics() {
        LatestCollection collection =
                loadOverallRankingStatisticsPort
                        .loadLatestAllConditionCollection()
                        .orElseThrow(() -> new OverallRankingStatisticsQueryException(
                                OverallRankingStatisticsQueryFailure.NOT_FOUND
                        ));

        List<ClassNameAggregate> aggregates =
                loadOverallRankingStatisticsPort.aggregateByClassName(
                        collection.collectionId()
                );

        long totalCount = aggregates.stream()
                .mapToLong(aggregate -> aggregate.count())
                .sum();

        if (totalCount != collection.sampleSize()) {
            throw new OverallRankingStatisticsQueryException(
                    OverallRankingStatisticsQueryFailure.DATA_INVALID
            );
        }

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
}
