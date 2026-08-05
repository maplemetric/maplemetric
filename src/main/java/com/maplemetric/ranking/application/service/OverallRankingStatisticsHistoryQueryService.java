package com.maplemetric.ranking.application.service;

import com.maplemetric.ranking.api.OverallRankingStatisticsHistoryQuery;
import com.maplemetric.ranking.api.OverallRankingStatisticsHistoryQueryException;
import com.maplemetric.ranking.api.OverallRankingStatisticsHistoryQueryFailure;
import com.maplemetric.ranking.api.OverallRankingStatisticsSnapshot;
import com.maplemetric.ranking.api.OverallRankingStatisticsSnapshot.JobCount;
import com.maplemetric.ranking.application.port.out.LoadOverallRankingStatisticsPort;
import com.maplemetric.ranking.application.port.out.LoadOverallRankingStatisticsPort.ClassNameAggregateByCollection;
import com.maplemetric.ranking.application.port.out.LoadOverallRankingStatisticsPort.LatestCollection;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OverallRankingStatisticsHistoryQueryService
        implements OverallRankingStatisticsHistoryQuery {

    private static final int MAX_RANGE_DAYS = 365;

    private final LoadOverallRankingStatisticsPort loadOverallRankingStatisticsPort;

    OverallRankingStatisticsHistoryQueryService(
            LoadOverallRankingStatisticsPort loadOverallRankingStatisticsPort
    ) {
        this.loadOverallRankingStatisticsPort =
                loadOverallRankingStatisticsPort;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OverallRankingStatisticsSnapshot> getJobStatisticsHistory(
            LocalDate from,
            LocalDate to
    ) {
        requireValidRange(from, to);

        return toSnapshots(
                loadOverallRankingStatisticsPort
                        .loadAllConditionCollectionsBetween(from, to)
        );
    }

    /**
     * 보존된 전체 기간을 조회한다.
     *
     * 기간 제한을 두지 않는 것 말고는 365일 조회와 같은 경로다. 시작점은 DB에 남아
     * 있는 최초 성공 Collection이며 호출자가 기간을 계산하지 않는다.
     */
    @Override
    @Transactional(readOnly = true)
    public List<OverallRankingStatisticsSnapshot> getJobStatisticsAllHistory() {
        return toSnapshots(
                loadOverallRankingStatisticsPort.loadAllConditionCollections()
        );
    }

    private List<OverallRankingStatisticsSnapshot> toSnapshots(
            List<LatestCollection> collections
    ) {
        if (collections.isEmpty()) {
            return List.of();
        }

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
            throw new OverallRankingStatisticsHistoryQueryException(
                    OverallRankingStatisticsHistoryQueryFailure.DATA_INVALID
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

    private void requireValidRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException(
                    "조회 시작일과 종료일은 필수입니다."
            );
        }

        if (from.isAfter(to)) {
            throw new IllegalArgumentException(
                    "조회 시작일은 종료일보다 늦을 수 없습니다."
            );
        }

        long rangeDays = ChronoUnit.DAYS.between(from, to) + 1;

        if (rangeDays > MAX_RANGE_DAYS) {
            throw new IllegalArgumentException(
                    "조회 기간은 " + MAX_RANGE_DAYS + "일 이하여야 합니다."
            );
        }
    }
}
