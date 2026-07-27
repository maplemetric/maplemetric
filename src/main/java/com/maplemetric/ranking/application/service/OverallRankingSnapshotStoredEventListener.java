package com.maplemetric.ranking.application.service;

import com.maplemetric.ranking.application.event.OverallRankingSnapshotStoredEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
public class OverallRankingSnapshotStoredEventListener {

    private final OverallRankingReferenceObserver overallRankingReferenceObserver;

    public OverallRankingSnapshotStoredEventListener(
            OverallRankingReferenceObserver overallRankingReferenceObserver
    ) {
        this.overallRankingReferenceObserver =
                overallRankingReferenceObserver;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStored(OverallRankingSnapshotStoredEvent event) {
        try {
            overallRankingReferenceObserver.observe(event);
        } catch (RuntimeException exception) {
            log.warn(
                    "종합 랭킹 기준정보 매핑 관측에 실패했습니다. 기준일={}",
                    event.snapshotDate(),
                    exception
            );
        }
    }
}
