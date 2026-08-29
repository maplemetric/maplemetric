package com.maplemetric.internal.infrastructure.scheduler;

import com.maplemetric.internal.application.properties.OverallRankingCollectionProperties;
import com.maplemetric.internal.application.service.OverallRankingGapRecoveryRunner;
import com.maplemetric.ranking.api.CollectOverallRankingSnapshotRequest;
import com.maplemetric.ranking.api.CollectOverallRankingSnapshotUseCase;
import com.maplemetric.ranking.api.OverallRankingCollectionAlreadyRunningException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log =
            LoggerFactory.getLogger(
                    OverallRankingCollectionScheduler.class
            );

    private final CollectOverallRankingSnapshotUseCase
            collectOverallRankingSnapshotUseCase;
    private final OverallRankingCollectionProperties properties;

    private final OverallRankingGapRecoveryRunner gapRecoveryRunner;

    public OverallRankingCollectionScheduler(
            CollectOverallRankingSnapshotUseCase
                    collectOverallRankingSnapshotUseCase,
            OverallRankingCollectionProperties properties,
            OverallRankingGapRecoveryRunner gapRecoveryRunner
    ) {
        this.collectOverallRankingSnapshotUseCase =
                collectOverallRankingSnapshotUseCase;
        this.properties = properties;
        this.gapRecoveryRunner = gapRecoveryRunner;
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

        recoverGaps();
    }

    /**
     * 오늘 몫을 받은 뒤에 비어 있는 지난 기준일을 메운다.
     *
     * 순서를 바꾸지 않는다. 메우기가 먼저 돌면 여러 번의 외부 호출을 쓴 뒤에야 오늘
     * 몫을 받게 되고, 그 사이 무슨 일이 생기면 정작 오늘이 빈다.
     *
     * 메우다 실패해도 여기서 삼킨다. 오늘 수집은 성공했는데 메우기에서 올라온 예외로
     * Scheduler가 ERROR로 남으면, 로그만 보고는 오늘 수집이 실패했다고 읽힌다.
     */
    private void recoverGaps() {
        try {
            gapRecoveryRunner.run();
        } catch (RuntimeException exception) {
            log.warn(
                    "비어 있는 기준일 메우기에 실패했습니다. 오늘 수집 결과는 "
                            + "그대로입니다.",
                    exception
            );
        }
    }
}
