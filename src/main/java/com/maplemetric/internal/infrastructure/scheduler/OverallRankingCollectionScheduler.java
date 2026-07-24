package com.maplemetric.internal.infrastructure.scheduler;

import com.maplemetric.internal.infrastructure.properties.OverallRankingCollectionProperties;
import com.maplemetric.ranking.api.CollectOverallRankingSnapshotRequest;
import com.maplemetric.ranking.api.CollectOverallRankingSnapshotUseCase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "maplemetric.internal.ranking"
                + ".overall-ranking-collection.scheduler",
        name = "enabled",
        havingValue = "true"
)
public class OverallRankingCollectionScheduler {

    private final CollectOverallRankingSnapshotUseCase
            collectOverallRankingSnapshotUseCase;
    private final OverallRankingCollectionProperties properties;

    public OverallRankingCollectionScheduler(
            CollectOverallRankingSnapshotUseCase
                    collectOverallRankingSnapshotUseCase,
            OverallRankingCollectionProperties properties
    ) {
        this.collectOverallRankingSnapshotUseCase =
                collectOverallRankingSnapshotUseCase;
        this.properties = properties;
    }

    @Scheduled(
            cron = "${maplemetric.internal.ranking"
                    + ".overall-ranking-collection.scheduler.cron}",
            zone = "${maplemetric.internal.ranking"
                    + ".overall-ranking-collection.scheduler.zone}"
    )
    public void collectOverallRanking() {
        collectOverallRankingSnapshotUseCase.collect(
                new CollectOverallRankingSnapshotRequest(
                        null,
                        properties.maxPages()
                )
        );
    }
}
