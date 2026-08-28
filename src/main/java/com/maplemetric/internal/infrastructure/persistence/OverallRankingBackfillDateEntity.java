package com.maplemetric.internal.infrastructure.persistence;

import com.maplemetric.internal.application.port.out.OverallRankingBackfillStatePort.BackfillErrorType;
import com.maplemetric.internal.application.port.out.OverallRankingBackfillStatePort.BackfillStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;

@Getter
@Entity
@Table(
        name = "p_overall_ranking_backfill_date",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_p_overall_ranking_backfill_date"
                                + "_job_snapshot_date",
                        columnNames = {"backfill_job_id", "snapshot_date"}
                )
        }
)
public class OverallRankingBackfillDateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "backfill_date_id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "backfill_job_id", nullable = false, updatable = false)
    private UUID backfillJobId;

    @Column(name = "snapshot_date", nullable = false, updatable = false)
    private LocalDate snapshotDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BackfillStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    /**
     * 지금 점유한 실행기를 가리키는 표다.
     *
     * 점유할 때마다 새로 발급한다. 결과를 기록할 때 이 표가 맞아야 상태를 바꾼다.
     * 기준일 번호만 보면, 오래 걸려 회수된 실행기의 뒤늦은 결과가 그 사이 새로
     * 점유한 실행기의 점유를 덮어쓴다.
     */
    @Column(name = "claim_token")
    private UUID claimToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_error_type", length = 30)
    private BackfillErrorType lastErrorType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    protected OverallRankingBackfillDateEntity() {
    }

    private OverallRankingBackfillDateEntity(
            UUID backfillJobId,
            LocalDate snapshotDate
    ) {
        if (backfillJobId == null) {
            throw new IllegalArgumentException(
                    "Backfill Job 식별자는 비어 있을 수 없습니다."
            );
        }

        if (snapshotDate == null) {
            throw new IllegalArgumentException(
                    "Backfill 기준일은 비어 있을 수 없습니다."
            );
        }

        this.backfillJobId = backfillJobId;
        this.snapshotDate = snapshotDate;
        this.status = BackfillStatus.PENDING;
    }

    public static OverallRankingBackfillDateEntity create(
            UUID backfillJobId,
            LocalDate snapshotDate
    ) {
        return new OverallRankingBackfillDateEntity(
                backfillJobId,
                snapshotDate
        );
    }

    /**
     * 실행기가 이 기준일을 점유한다.
     *
     * 시도 횟수를 올려 무한 재시도를 실행기가 판단할 수 있게 한다.
     */
    public void claim() {
        status = BackfillStatus.RUNNING;
        attemptCount++;
        startedAt = Instant.now();
        finishedAt = null;
        claimToken = UUID.randomUUID();
    }

    /**
     * 결과 기록은 점유 중인 기준일에만 적용한다.
     *
     * 실행기가 외부 호출을 하는 동안 Job이 취소될 수 있다. 상태를 확인하지 않으면
     * 취소된 기준일이 되살아나고 Job 집계까지 어긋난다. 이미 끝난 기준일이면 아무것도
     * 바꾸지 않고 {@code false}를 돌려 호출자가 집계를 올리지 않게 한다.
     */
    public boolean succeed(UUID expectedClaimToken) {
        if (!ownsClaim(expectedClaimToken)) {
            return false;
        }

        return finishIfRunning(BackfillStatus.SUCCEEDED, null);
    }

    public boolean skip(UUID expectedClaimToken) {
        if (!ownsClaim(expectedClaimToken)) {
            return false;
        }

        return finishIfRunning(BackfillStatus.SKIPPED, null);
    }

    /**
     * 결과를 기록하려는 쪽이 지금 점유한 실행기인지 본다.
     *
     * 표가 없는 기준일은 이 표를 쓰기 전에 점유된 것이다. 그때는 누구도 결과를
     * 기록하지 못하게 막는다. 잘못 덮어쓰는 것보다 회수 경로가 다시 잡게 두는 편이
     * 안전하다. 회수는 점유 시각만 보므로 표가 없어도 동작한다.
     */
    private boolean ownsClaim(UUID expectedClaimToken) {
        return claimToken != null
                && claimToken.equals(expectedClaimToken);
    }

    /**
     * 시도를 실패로 기록한다.
     *
     * 재시도 가능하면 PENDING으로 되돌려 다시 점유되게 한다. 이때는 종료 시각을 남기지
     * 않는다. 아직 끝난 기준일이 아니기 때문이다.
     */
    public boolean fail(
            UUID expectedClaimToken,
            BackfillErrorType errorType,
            boolean retryable
    ) {
        if (errorType == null) {
            throw new IllegalArgumentException(
                    "Backfill 오류 분류는 비어 있을 수 없습니다."
            );
        }

        if (status != BackfillStatus.RUNNING
                || !ownsClaim(expectedClaimToken)) {
            return false;
        }

        if (retryable) {
            lastErrorType = errorType;
            status = BackfillStatus.PENDING;
            finishedAt = null;
            claimToken = null;
            return true;
        }

        finishWith(BackfillStatus.FAILED, errorType);

        return true;
    }

    /**
     * 점유를 되돌린다.
     *
     * 점유할 때 오른 시도를 함께 되돌린다. 이 기준일을 시험한 적이 없기 때문이다.
     * 되돌리지 않으면 기준일과 무관한 사정이 반복될수록 시도가 쌓이고, 나중에 진짜
     * 일시 오류가 왔을 때 이미 소진돼 영구 실패한다.
     */
    public boolean release(
            UUID expectedClaimToken,
            BackfillErrorType errorType
    ) {
        if (status != BackfillStatus.RUNNING
                || !ownsClaim(expectedClaimToken)) {
            return false;
        }

        lastErrorType = errorType;
        status = BackfillStatus.PENDING;
        finishedAt = null;
        claimToken = null;

        if (attemptCount > 0) {
            attemptCount--;
        }

        return true;
    }

    public void cancel() {
        if (isTerminal()) {
            return;
        }

        finishWith(BackfillStatus.CANCELLED, lastErrorType);
    }

    public boolean isTerminal() {
        return status == BackfillStatus.SUCCEEDED
                || status == BackfillStatus.FAILED
                || status == BackfillStatus.SKIPPED
                || status == BackfillStatus.CANCELLED;
    }

    private boolean finishIfRunning(
            BackfillStatus terminalStatus,
            BackfillErrorType errorType
    ) {
        if (status != BackfillStatus.RUNNING) {
            return false;
        }

        finishWith(terminalStatus, errorType);

        return true;
    }

    /**
     * 기준일을 끝난 상태로 닫는다.
     *
     * 점유 표도 함께 지운다. 표가 남아 있는 것은 지금 누군가 점유 중이라는 뜻이라,
     * 끝난 기준일에 남겨 두면 표의 의미가 흐려진다.
     */
    private void finishWith(
            BackfillStatus terminalStatus,
            BackfillErrorType errorType
    ) {
        status = terminalStatus;
        lastErrorType = errorType;
        finishedAt = Instant.now();
        claimToken = null;
    }

    @PrePersist
    private void prePersist() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    private void preUpdate() {
        updatedAt = Instant.now();
    }
}
