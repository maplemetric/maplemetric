package com.maplemetric.ranking.application.service;

import com.maplemetric.ranking.api.ExpireOverallRankingSnapshotUseCase;
import com.maplemetric.ranking.api.OverallRankingRetentionPlan;
import com.maplemetric.ranking.api.OverallRankingRetentionRequest;
import com.maplemetric.ranking.application.port.out.ExpireOverallRankingSnapshotPort;
import com.maplemetric.ranking.application.port.out.ExpireOverallRankingSnapshotPort.DeletedCounts;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 보존 기간이 지난 수집 결과를 만료시킨다.
 *
 * 최신 기준일은 언제나 남긴다. 보존 일수를 잘못 설정해도 조회가 통째로 비지 않게
 * 하는 마지막 방어선이다.
 *
 * 한 번의 실행은 {@code maxDatesPerRun}개 기준일만 지운다. 남은 기준일은 다음 실행이
 * 이어서 처리하며, 그래야 오래 잠긴 대량 삭제가 수집·조회를 막지 않는다.
 */
@Slf4j
@Service
public class OverallRankingRetentionService
        implements ExpireOverallRankingSnapshotUseCase {

    private final ExpireOverallRankingSnapshotPort expirePort;

    public OverallRankingRetentionService(
            ExpireOverallRankingSnapshotPort expirePort
    ) {
        this.expirePort = expirePort;
    }

    @Override
    @Transactional(readOnly = true)
    public OverallRankingRetentionPlan plan(
            OverallRankingRetentionRequest request
    ) {
        Optional<LocalDate> retainedDate =
                expirePort.findLatestSnapshotDate();

        if (retainedDate.isEmpty()) {
            return OverallRankingRetentionPlan.empty(null);
        }

        List<LocalDate> targetDates = findTargetDates(
                request,
                retainedDate.get()
        );

        if (targetDates.isEmpty()) {
            return OverallRankingRetentionPlan.empty(retainedDate.get());
        }

        return new OverallRankingRetentionPlan(
                targetDates,
                expirePort.countCollections(targetDates),
                expirePort.countSnapshots(targetDates),
                retainedDate.get()
        );
    }

    /**
     * 산정과 삭제를 한 Transaction에서 처리한다.
     *
     * 산정 결과를 다른 Transaction에서 지우면 그 사이에 들어온 수집이 대상에 섞이거나
     * 이미 지워진 기준일을 다시 지우려 든다.
     */
    @Override
    @Transactional
    public OverallRankingRetentionPlan expire(
            OverallRankingRetentionRequest request
    ) {
        Optional<LocalDate> retainedDate =
                expirePort.findLatestSnapshotDate();

        if (retainedDate.isEmpty()) {
            return OverallRankingRetentionPlan.empty(null);
        }

        List<LocalDate> targetDates = findTargetDates(
                request,
                retainedDate.get()
        );

        if (targetDates.isEmpty()) {
            return OverallRankingRetentionPlan.empty(retainedDate.get());
        }

        DeletedCounts deleted =
                expirePort.deleteBySnapshotDates(targetDates);

        log.warn(
                "Overall Ranking 수집 결과를 만료시켰습니다. "
                        + "기준일={}~{}, 기준일 수={}, Collection={}, Snapshot={}, "
                        + "보존한 최신 기준일={}",
                targetDates.get(0),
                targetDates.get(targetDates.size() - 1),
                targetDates.size(),
                deleted.collectionCount(),
                deleted.snapshotCount(),
                retainedDate.get()
        );

        return new OverallRankingRetentionPlan(
                targetDates,
                deleted.collectionCount(),
                deleted.snapshotCount(),
                retainedDate.get()
        );
    }

    private List<LocalDate> findTargetDates(
            OverallRankingRetentionRequest request,
            LocalDate retainedDate
    ) {
        return expirePort.findExpirableSnapshotDates(
                request.expireBefore(),
                retainedDate,
                request.maxDatesPerRun()
        );
    }
}
