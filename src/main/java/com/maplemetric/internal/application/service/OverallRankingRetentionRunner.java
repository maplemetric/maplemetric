package com.maplemetric.internal.application.service;

import com.maplemetric.internal.application.properties.OverallRankingRetentionProperties;
import com.maplemetric.ranking.api.ExpireOverallRankingSnapshotUseCase;
import com.maplemetric.ranking.api.OverallRankingRetentionPlan;
import com.maplemetric.ranking.api.OverallRankingRetentionRequest;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 설정된 보존 기간으로 만료 대상을 산정하고 실행한다.
 *
 * 이 클래스는 스스로 실행되지 않는다. Scheduler도 Endpoint도 두지 않았다. 삭제는
 * 되돌릴 수 없고 보존 정책이 아직 확정되지 않았으므로, 실행 경로는 정책 승인 뒤에
 * 붙인다.
 */
@Slf4j
@Service
public class OverallRankingRetentionRunner {

    private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");

    private final ExpireOverallRankingSnapshotUseCase expireUseCase;
    private final OverallRankingRetentionProperties properties;
    private final Clock clock;

    @Autowired
    public OverallRankingRetentionRunner(
            ExpireOverallRankingSnapshotUseCase expireUseCase,
            OverallRankingRetentionProperties properties
    ) {
        this(expireUseCase, properties, Clock.system(KOREA_ZONE_ID));
    }

    OverallRankingRetentionRunner(
            ExpireOverallRankingSnapshotUseCase expireUseCase,
            OverallRankingRetentionProperties properties,
            Clock clock
    ) {
        this.expireUseCase = expireUseCase;
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * 무엇이 지워질지만 계산한다.
     *
     * 아무것도 바꾸지 않으므로 삭제가 꺼져 있어도 호출할 수 있다. 실행 전에 대상을
     * 눈으로 확인하는 경로다.
     */
    public OverallRankingRetentionPlan plan() {
        OverallRankingRetentionPlan plan =
                expireUseCase.plan(createRequest());

        log.info(
                "만료 대상을 산정했습니다. 보존 일수={}, 기준일 수={}, "
                        + "Collection={}, Snapshot={}, 보존한 최신 기준일={}",
                properties.retentionDays(),
                plan.snapshotDates().size(),
                plan.collectionCount(),
                plan.snapshotCount(),
                plan.retainedLatestDate()
        );

        return plan;
    }

    /**
     * 대상을 실제로 삭제한다.
     *
     * 설정이 꺼져 있으면 아무것도 지우지 않는다. 보존 정책과 복구 수단이 확정되기
     * 전에는 이 경로가 열리지 않아야 한다.
     */
    public OverallRankingRetentionPlan expire() {
        if (!properties.enabled()) {
            throw new OverallRankingRetentionDisabledException();
        }

        return expireUseCase.expire(createRequest());
    }

    private OverallRankingRetentionRequest createRequest() {
        return new OverallRankingRetentionRequest(
                LocalDate.now(clock).minusDays(properties.retentionDays()),
                properties.maxDatesPerRun()
        );
    }
}
