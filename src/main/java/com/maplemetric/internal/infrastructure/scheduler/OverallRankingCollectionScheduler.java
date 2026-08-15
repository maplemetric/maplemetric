package com.maplemetric.internal.infrastructure.scheduler;

import com.maplemetric.internal.application.properties.OverallRankingCollectionProperties;
import com.maplemetric.ranking.api.CollectOverallRankingSnapshotRequest;
import com.maplemetric.ranking.api.CollectOverallRankingSnapshotUseCase;
import com.maplemetric.ranking.api.OverallRankingCollectionAlreadyRunningException;
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
        try {
            collectOverallRankingSnapshotUseCase.collect(
                    new CollectOverallRankingSnapshotRequest(
                            null,
                            properties.maxPages()
                    )
            );
        } catch (OverallRankingCollectionAlreadyRunningException exception) {
            // 중복 실행 차단은 정상 동작이므로 Scheduler 기본 Handler로
            // 전파해 ERROR로 기록되지 않게 한다. 로그는 UseCase Bridge가 남긴다.
        }
    }
}
