package com.maplemetric.internal.infrastructure.persistence;

import com.maplemetric.internal.application.port.out.OverallRankingBackfillStatePort;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class OverallRankingBackfillStatePersistenceAdapter
        implements OverallRankingBackfillStatePort {

    private final OverallRankingBackfillJobRepository jobRepository;
    private final OverallRankingBackfillDateRepository dateRepository;

    OverallRankingBackfillStatePersistenceAdapter(
            OverallRankingBackfillJobRepository jobRepository,
            OverallRankingBackfillDateRepository dateRepository
    ) {
        this.jobRepository = jobRepository;
        this.dateRepository = dateRepository;
    }

    @Override
    public BackfillJob createJob(
            LocalDate requestedFrom,
            LocalDate requestedTo
    ) {
        OverallRankingBackfillJobEntity job = jobRepository.save(
                OverallRankingBackfillJobEntity.create(
                        requestedFrom,
                        requestedTo
                )
        );

        // 같은 Transaction에서 곧바로 점유해도 Native Query가 행을 볼 수 있어야 한다.
        dateRepository.saveAllAndFlush(createDates(job));

        return toBackfillJob(job);
    }

    @Override
    public Optional<BackfillJob> findJob(UUID backfillJobId) {
        return jobRepository.findById(backfillJobId)
                .map(job -> toBackfillJob(job));
    }

    @Override
    public List<BackfillDate> findDates(UUID backfillJobId) {
        return dateRepository
                .findByBackfillJobIdOrderBySnapshotDateAsc(backfillJobId)
                .stream()
                .map(date -> toBackfillDate(date))
                .toList();
    }

    @Override
    public List<BackfillDate> findStaleClaims(
            UUID backfillJobId,
            Instant claimedBefore
    ) {
        return dateRepository
                .findStaleClaims(backfillJobId, claimedBefore)
                .stream()
                .map(date -> toBackfillDate(date))
                .toList();
    }

    @Override
    public Optional<BackfillDate> claimNextPendingDate(UUID backfillJobId) {
        Optional<OverallRankingBackfillDateEntity> claimed =
                dateRepository.lockNextPending(backfillJobId);

        if (claimed.isEmpty()) {
            return Optional.empty();
        }

        OverallRankingBackfillDateEntity date = claimed.get();
        UUID claimedDateId = date.getId();

        date.claim();

        // 조건부 UPDATE가 영속성 컨텍스트를 비우므로 점유 결과를 다시 읽는다.
        jobRepository.startIfPending(
                backfillJobId,
                BackfillStatus.RUNNING,
                BackfillStatus.PENDING,
                Instant.now()
        );

        return Optional.of(toBackfillDate(getDate(claimedDateId)));
    }

    @Override
    public void succeedDate(UUID backfillDateId, UUID claimToken) {
        OverallRankingBackfillDateEntity date = getDate(backfillDateId);

        if (!date.succeed(claimToken)) {
            return;
        }

        jobRepository.increaseSucceededDateCount(date.getBackfillJobId());
        finishJobIfDone(date.getBackfillJobId());
    }

    @Override
    public void skipDate(UUID backfillDateId, UUID claimToken) {
        OverallRankingBackfillDateEntity date = getDate(backfillDateId);

        if (!date.skip(claimToken)) {
            return;
        }

        jobRepository.increaseSkippedDateCount(date.getBackfillJobId());
        finishJobIfDone(date.getBackfillJobId());
    }

    @Override
    public void releaseDate(
            UUID backfillDateId,
            UUID claimToken,
            BackfillErrorType errorType
    ) {
        getDate(backfillDateId).release(claimToken, errorType);
    }

    @Override
    public void failDate(
            UUID backfillDateId,
            UUID claimToken,
            BackfillErrorType errorType,
            boolean retryable
    ) {
        OverallRankingBackfillDateEntity date = getDate(backfillDateId);

        if (!date.fail(claimToken, errorType, retryable) || retryable) {
            return;
        }

        jobRepository.increaseFailedDateCount(date.getBackfillJobId());
        finishJobIfDone(date.getBackfillJobId());
    }

    @Override
    public void cancelJob(UUID backfillJobId) {
        // 잠금은 기준일 다음 Job 순서로만 잡는다. 다른 경로도 기준일을 먼저 잡으므로
        // 순서를 뒤집으면 취소와 결과 기록이 서로를 기다려 교착이 난다.
        List<OverallRankingBackfillDateEntity> dates =
                dateRepository.findByBackfillJobIdForUpdate(backfillJobId);

        OverallRankingBackfillJobEntity job = getJobForUpdate(backfillJobId);

        dates.forEach(date -> date.cancel());

        job.cancel();
    }

    private List<OverallRankingBackfillDateEntity> createDates(
            OverallRankingBackfillJobEntity job
    ) {
        List<OverallRankingBackfillDateEntity> dates = new ArrayList<>();

        for (
                LocalDate snapshotDate = job.getRequestedFrom();
                !snapshotDate.isAfter(job.getRequestedTo());
                snapshotDate = snapshotDate.plusDays(1)
        ) {
            dates.add(OverallRankingBackfillDateEntity.create(
                    job.getId(),
                    snapshotDate
            ));
        }

        return dates;
    }

    /**
     * 남은 기준일이 없으면 Job을 종료한다.
     *
     * 실패한 기준일이 하나라도 있으면 Job도 실패로 남긴다.
     */
    private void finishJobIfDone(UUID backfillJobId) {
        if (dateRepository.countUnfinished(backfillJobId) > 0L) {
            return;
        }

        getJobForUpdate(backfillJobId).finish(
                dateRepository.countFailed(backfillJobId) > 0L
        );
    }

    private OverallRankingBackfillJobEntity getJobForUpdate(
            UUID backfillJobId
    ) {
        return jobRepository.findByIdForUpdate(backfillJobId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Backfill Job을 찾을 수 없습니다."
                ));
    }

    /**
     * 결과를 기록할 기준일을 잠근 채로 읽는다.
     *
     * 잠금 순서는 기준일 다음 Job이다. 결과 기록도 취소도 같은 순서를 지켜야 서로를
     * 기다리며 교착에 빠지지 않는다.
     */
    private OverallRankingBackfillDateEntity getDate(UUID backfillDateId) {
        return dateRepository.findByIdForUpdate(backfillDateId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Backfill 기준일 항목을 찾을 수 없습니다."
                ));
    }

    private BackfillJob toBackfillJob(
            OverallRankingBackfillJobEntity job
    ) {
        return new BackfillJob(
                job.getId(),
                job.getRequestedFrom(),
                job.getRequestedTo(),
                job.getStatus(),
                job.getSucceededDateCount(),
                job.getFailedDateCount(),
                job.getSkippedDateCount(),
                job.getCreatedAt(),
                job.getStartedAt(),
                job.getFinishedAt()
        );
    }

    private BackfillDate toBackfillDate(
            OverallRankingBackfillDateEntity date
    ) {
        return new BackfillDate(
                date.getId(),
                date.getBackfillJobId(),
                date.getSnapshotDate(),
                date.getStatus(),
                date.getAttemptCount(),
                date.getLastErrorType(),
                date.getClaimToken(),
                date.getStartedAt(),
                date.getFinishedAt()
        );
    }
}
