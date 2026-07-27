package com.maplemetric.ranking.application.service;

import com.maplemetric.ranking.api.OverallRankingStatisticsSnapshot;
import com.maplemetric.ranking.api.OverallRankingStatisticsSnapshot.JobCount;
import com.maplemetric.ranking.api.OverallRankingStatisticsTrendQuery;
import com.maplemetric.ranking.api.OverallRankingStatisticsTrendQueryException;
import com.maplemetric.ranking.api.OverallRankingStatisticsTrendQueryFailure;
import com.maplemetric.ranking.application.port.out.LoadOverallRankingStatisticsPort;
import com.maplemetric.ranking.application.port.out.LoadOverallRankingStatisticsPort.ClassNameAggregateByCollection;
import com.maplemetric.ranking.application.port.out.LoadOverallRankingStatisticsPort.LatestCollection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OverallRankingStatisticsTrendQueryService
        implements OverallRankingStatisticsTrendQuery {

    private static final int MIN_DAYS = 1;
    private static final int MAX_DAYS = 31;

    private final LoadOverallRankingStatisticsPort loadOverallRankingStatisticsPort;

    OverallRankingStatisticsTrendQueryService(
            LoadOverallRankingStatisticsPort loadOverallRankingStatisticsPort
    ) {
        this.loadOverallRankingStatisticsPort =
                loadOverallRankingStatisticsPort;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OverallRankingStatisticsSnapshot> getJobStatisticsTrend(
            int days
    ) {
        requireValidDays(days);

        LatestCollection latest = loadOverallRankingStatisticsPort
                .loadLatestAllConditionCollection()
                .orElseThrow(() -> new OverallRankingStatisticsTrendQueryException(
                        OverallRankingStatisticsTrendQueryFailure.NOT_FOUND
                ));

        List<LatestCollection> collections = loadOverallRankingStatisticsPort
                .loadAllConditionCollectionsWithin(
                        latest.snapshotDate(),
                        days
                );

        List<UUID> collectionIds = collections.stream()
                .map(collection -> collection.collectionId())
                .toList();

        Map<UUID, List<ClassNameAggregateByCollection>> aggregatesByCollectionId =
                loadOverallRankingStatisticsPort
                        .aggregateByClassName(collectionIds)
                        .stream()
                        .collect(Collectors.groupingBy(
                                aggregate -> aggregate.collectionId()
                        ));

        return collections.stream()
                .map(collection -> toSnapshot(
                        collection,
                        aggregatesByCollectionId.getOrDefault(
                                collection.collectionId(),
                                List.of()
                        )
                ))
                .sorted(Comparator.comparing(
                        OverallRankingStatisticsSnapshot::asOf
                ))
                .toList();
    }

    private OverallRankingStatisticsSnapshot toSnapshot(
            LatestCollection collection,
            List<ClassNameAggregateByCollection> aggregates
    ) {
        long totalCount = aggregates.stream()
                .mapToLong(aggregate -> aggregate.count())
                .sum();

        if (totalCount != collection.sampleSize()) {
            throw new OverallRankingStatisticsTrendQueryException(
                    OverallRankingStatisticsTrendQueryFailure.DATA_INVALID
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

    private void requireValidDays(int days) {
        if (days < MIN_DAYS || days > MAX_DAYS) {
            throw new IllegalArgumentException(
                    "조회 일수는 " + MIN_DAYS + " 이상 "
                            + MAX_DAYS + " 이하여야 합니다."
            );
        }
    }
}
