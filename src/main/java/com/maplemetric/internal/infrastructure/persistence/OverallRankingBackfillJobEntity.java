package com.maplemetric.internal.infrastructure.persistence;

import com.maplemetric.internal.application.port.out.OverallRankingBackfillStatePort.BackfillStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;

@Getter
@Entity
@Table(name = "p_overall_ranking_backfill_job")
public class OverallRankingBackfillJobEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "backfill_job_id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "requested_from", nullable = false, updatable = false)
    private LocalDate requestedFrom;

    @Column(name = "requested_to", nullable = false, updatable = false)
    private LocalDate requestedTo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BackfillStatus status;

    @Column(name = "succeeded_date_count", nullable = false)
    private int succeededDateCount;

    @Column(name = "failed_date_count", nullable = false)
    private int failedDateCount;

    @Column(name = "skipped_date_count", nullable = false)
    private int skippedDateCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    protected OverallRankingBackfillJobEntity() {
    }

    private OverallRankingBackfillJobEntity(
            LocalDate requestedFrom,
            LocalDate requestedTo
    ) {
        if (requestedFrom == null || requestedTo == null) {
            throw new IllegalArgumentException(
                    "Backfill 요청 기간은 비어 있을 수 없습니다."
            );
        }

        if (requestedFrom.isAfter(requestedTo)) {
            throw new IllegalArgumentException(
                    "Backfill 시작일은 종료일보다 뒤일 수 없습니다."
            );
        }

        this.requestedFrom = requestedFrom;
        this.requestedTo = requestedTo;
        this.status = BackfillStatus.PENDING;
    }

    public static OverallRankingBackfillJobEntity create(
            LocalDate requestedFrom,
            LocalDate requestedTo
    ) {
        return new OverallRankingBackfillJobEntity(
                requestedFrom,
                requestedTo
        );
    }

    /**
     * 남은 기준일이 없을 때 Job을 종료한다.
     *
     * 실패한 기준일이 하나라도 있으면 Job도 실패다. 부분 성공을 성공으로 보고하지
     * 않는다.
     */
    public void finish(boolean hasFailedDate) {
        if (isTerminal()) {
            return;
        }

        status = hasFailedDate
                ? BackfillStatus.FAILED
                : BackfillStatus.SUCCEEDED;
        finishedAt = Instant.now();
    }

    public void cancel() {
        if (isTerminal()) {
            return;
        }

        status = BackfillStatus.CANCELLED;
        finishedAt = Instant.now();
    }

    public boolean isTerminal() {
        return status == BackfillStatus.SUCCEEDED
                || status == BackfillStatus.FAILED
                || status == BackfillStatus.CANCELLED;
    }

    @PrePersist
    private void prePersist() {
        createdAt = Instant.now();
    }
}
