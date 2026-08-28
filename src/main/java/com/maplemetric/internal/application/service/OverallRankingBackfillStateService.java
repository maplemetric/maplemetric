package com.maplemetric.internal.application.service;

import com.maplemetric.internal.application.port.out.OverallRankingBackfillStatePort;
import com.maplemetric.internal.application.port.out.OverallRankingBackfillStatePort.BackfillDate;
import com.maplemetric.internal.application.port.out.OverallRankingBackfillStatePort.BackfillErrorType;
import com.maplemetric.internal.application.port.out.OverallRankingBackfillStatePort.BackfillJob;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Backfill 실행 상태를 짧은 Transaction으로 기록한다.
 *
 * 각 public 메서드가 하나의 상태 전이만 담당한다. Nexon 호출과 수집 저장은 이 경계
 * 밖에서 일어나며, 외부 호출을 이 Transaction에 포함하지 않는다.
 */
@Service
public class OverallRankingBackfillStateService {

    private final OverallRankingBackfillStatePort backfillStatePort;

    public OverallRankingBackfillStateService(
            OverallRankingBackfillStatePort backfillStatePort
    ) {
        this.backfillStatePort = backfillStatePort;
    }

    @Transactional
    public BackfillJob createJob(
            LocalDate requestedFrom,
            LocalDate requestedTo
    ) {
        return backfillStatePort.createJob(requestedFrom, requestedTo);
    }

    @Transactional(readOnly = true)
    public Optional<BackfillJob> findJob(UUID backfillJobId) {
        return backfillStatePort.findJob(backfillJobId);
    }

    @Transactional(readOnly = true)
    public List<BackfillDate> findDates(UUID backfillJobId) {
        return backfillStatePort.findDates(backfillJobId);
    }

    @Transactional(readOnly = true)
    public List<BackfillDate> findStaleClaims(
            UUID backfillJobId,
            Instant claimedBefore
    ) {
        return backfillStatePort.findStaleClaims(
                backfillJobId,
                claimedBefore
        );
    }

    /**
     * 다음 기준일을 점유한다.
     *
     * 점유는 외부 호출 전에 짧게 끝나야 하므로 이 Transaction 안에서 수집을 하지
     * 않는다. 반환된 기준일의 수집이 끝나면 결과 기록 메서드를 따로 호출한다.
     */
    @Transactional
    public Optional<BackfillDate> claimNextPendingDate(UUID backfillJobId) {
        return backfillStatePort.claimNextPendingDate(backfillJobId);
    }

    @Transactional
    public void succeedDate(UUID backfillDateId) {
        backfillStatePort.succeedDate(backfillDateId);
    }

    @Transactional
    public void skipDate(UUID backfillDateId) {
        backfillStatePort.skipDate(backfillDateId);
    }

    @Transactional
    public void failDate(
            UUID backfillDateId,
            BackfillErrorType errorType,
            boolean retryable
    ) {
        backfillStatePort.failDate(backfillDateId, errorType, retryable);
    }

    @Transactional
    public void releaseDate(
            UUID backfillDateId,
            BackfillErrorType errorType
    ) {
        backfillStatePort.releaseDate(backfillDateId, errorType);
    }

    @Transactional
    public void cancelJob(UUID backfillJobId) {
        backfillStatePort.cancelJob(backfillJobId);
    }
}
