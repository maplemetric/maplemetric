package com.maplemetric.internal.infrastructure.scheduler;

import com.maplemetric.internal.application.properties.OverallRankingCollectionProperties;
import com.maplemetric.internal.application.service.OverallRankingGapRecoveryRunner;
import com.maplemetric.ranking.api.CollectOverallRankingSnapshotOutcome;
import com.maplemetric.ranking.api.CollectOverallRankingSnapshotRequest;
import com.maplemetric.ranking.api.CollectOverallRankingSnapshotUseCase;
import com.maplemetric.ranking.api.OverallRankingCollectionAlreadyRunningException;
import com.maplemetric.ranking.api.OverallRankingCollectionException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
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

    private static final String SCHEDULED_COLLECTION_METRIC_NAME =
            "ranking.collection.scheduled";

    private static final String SUCCEEDED_RESULT = "succeeded";

    private static final String SKIPPED_RESULT = "skipped";

    private static final String FAILED_RESULT = "failed";

    /** 사유가 없는 결과에도 같은 Tag를 붙인다. Tag가 빠지면 계열이 갈린다. */
    private static final String NO_REASON = "none";

    /** 같은 기준일을 이미 받아 두어 건너뛴 것이다. */
    private static final String ALREADY_COLLECTED_REASON = "ALREADY_COLLECTED";

    /** 다른 수집이 도는 중이라 건너뛴 것이다. */
    private static final String ALREADY_RUNNING_REASON = "ALREADY_RUNNING";

    private static final Logger log =
            LoggerFactory.getLogger(
                    OverallRankingCollectionScheduler.class
            );

    private final CollectOverallRankingSnapshotUseCase
            collectOverallRankingSnapshotUseCase;
    private final OverallRankingCollectionProperties properties;

    private final OverallRankingGapRecoveryRunner gapRecoveryRunner;

    private final MeterRegistry meterRegistry;

    public OverallRankingCollectionScheduler(
            CollectOverallRankingSnapshotUseCase
                    collectOverallRankingSnapshotUseCase,
            OverallRankingCollectionProperties properties,
            OverallRankingGapRecoveryRunner gapRecoveryRunner,
            MeterRegistry meterRegistry
    ) {
        this.collectOverallRankingSnapshotUseCase =
                collectOverallRankingSnapshotUseCase;
        this.properties = properties;
        this.gapRecoveryRunner = gapRecoveryRunner;
        this.meterRegistry = meterRegistry;
    }

    @Scheduled(
            cron = "${maplemetric.internal.ranking"
                    + ".overall-ranking-collection.scheduler.cron}",
            zone = "${maplemetric.internal.ranking"
                    + ".overall-ranking-collection.scheduler.zone}"
    )
    public void collectOverallRanking() {
        try {
            CollectOverallRankingSnapshotOutcome outcome =
                    collectOverallRankingSnapshotUseCase.collect(
                            new CollectOverallRankingSnapshotRequest(
                                    null,
                                    properties.maxPages()
                            )
                    );

            // 이미 받아 둔 기준일이면 예외 없이 건너뛴 결과가 돌아온다. 이것을
            // 성공으로 세면 새로 받은 날과 구별되지 않아, 며칠째 같은 자리에
            // 머물러 있어도 매일 성공한 것처럼 보인다.
            countOutcome(outcome);
        } catch (OverallRankingCollectionAlreadyRunningException exception) {
            // 중복 실행 차단은 정상 동작이므로 Scheduler 기본 Handler로
            // 전파해 ERROR로 기록되지 않게 한다. 로그는 UseCase Bridge가 남긴다.
            count(SKIPPED_RESULT, ALREADY_RUNNING_REASON);
        } catch (OverallRankingCollectionException exception) {
            count(FAILED_RESULT, exception.getFailure().name());

            throw exception;
        } catch (RuntimeException exception) {
            count(FAILED_RESULT, NO_REASON);

            throw exception;
        }

        recoverGaps();
    }

    private void countOutcome(CollectOverallRankingSnapshotOutcome outcome) {
        if (outcome != null
                && outcome.status()
                        == com.maplemetric.ranking.api
                                .OverallRankingCollectionStatus.SKIPPED) {
            count(SKIPPED_RESULT, ALREADY_COLLECTED_REASON);

            return;
        }

        count(SUCCEEDED_RESULT, NO_REASON);
    }

    /**
     * 정기 수집의 결과를 센다.
     *
     * 실패 사유를 함께 남긴다. 실패 수만 세면 외부가 잠시 막힌 것과 응답이 계약과
     * 어긋난 것이 같은 숫자로 보인다. 그 둘은 대응이 다르다.
     *
     * 지표를 남기다 실패해도 수집 결과를 뒤집지 않는다. 무엇이 일어났는지 적는 일이
     * 일어난 일 자체를 되돌릴 이유는 없다.
     */
    private void count(String result, String reason) {
        try {
            Counter.builder(SCHEDULED_COLLECTION_METRIC_NAME)
                    .tag("result", result)
                    .tag("reason", reason)
                    .description("정기 랭킹 수집의 실행 결과 수다.")
                    .register(meterRegistry)
                    .increment();
        } catch (RuntimeException exception) {
            log.warn("수집 결과 지표를 남기지 못했습니다.", exception);
        }
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
