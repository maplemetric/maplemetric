package com.maplemetric.internal.infrastructure.persistence;

import com.maplemetric.internal.application.port.out.OverallRankingBackfillStatePort;
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

        dateRepository.saveAll(createDates(job));

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
    public Optional<BackfillDate> claimNextPendingDate(UUID backfillJobId) {
        Optional<OverallRankingBackfillDateEntity> claimed =
                dateRepository.lockNextPending(backfillJobId);

        if (claimed.isEmpty()) {
            return Optional.empty();
        }

        OverallRankingBackfillDateEntity date = claimed.get();
        date.claim();

        jobRepository.findById(backfillJobId)
                .ifPresent(job -> job.startIfPending());

        return Optional.of(toBackfillDate(date));
    }

    @Override
    public void succeedDate(UUID backfillDateId) {
        OverallRankingBackfillDateEntity date = getDate(backfillDateId);

        date.succeed();
        jobRepository.increaseSucceededDateCount(date.getBackfillJobId());
        finishJobIfDone(date.getBackfillJobId());
    }

    @Override
    public void skipDate(UUID backfillDateId) {
        OverallRankingBackfillDateEntity date = getDate(backfillDateId);

        date.skip();
        jobRepository.increaseSkippedDateCount(date.getBackfillJobId());
        finishJobIfDone(date.getBackfillJobId());
    }

    @Override
    public void failDate(
            UUID backfillDateId,
            BackfillErrorType errorType,
            boolean retryable
    ) {
        OverallRankingBackfillDateEntity date = getDate(backfillDateId);

        date.fail(errorType, retryable);

        if (retryable) {
            return;
        }

        jobRepository.increaseFailedDateCount(date.getBackfillJobId());
        finishJobIfDone(date.getBackfillJobId());
    }

    @Override
    public void cancelJob(UUID backfillJobId) {
        dateRepository
                .findByBackfillJobIdOrderBySnapshotDateAsc(backfillJobId)
                .forEach(date -> date.cancel());

        jobRepository.findById(backfillJobId)
                .ifPresent(job -> job.cancel());
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

        jobRepository.findById(backfillJobId)
                .ifPresent(job -> job.finish(
                        dateRepository.countFailed(backfillJobId) > 0L
                ));
    }

    private OverallRankingBackfillDateEntity getDate(UUID backfillDateId) {
        return dateRepository.findById(backfillDateId)
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
                date.getStartedAt(),
                date.getFinishedAt()
        );
    }
}
