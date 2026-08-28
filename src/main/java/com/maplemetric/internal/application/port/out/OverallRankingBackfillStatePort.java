package com.maplemetric.internal.application.port.out;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Overall Ranking Backfill 실행 상태를 저장한다.
 *
 * 실제 수집 실행은 이 계약의 범위가 아니다. 여기서는 어떤 기준일이 남았고 무엇이
 * 끝났는지를 명시적으로 남겨 중단 후 재개할 수 있게 하는 것만 다룬다.
 *
 * 저장된 성공 Snapshot과 실행 상태는 소유자가 다르다. 이 계약은 Snapshot을 읽거나
 * 쓰지 않는다.
 */
public interface OverallRankingBackfillStatePort {

    BackfillJob createJob(
            LocalDate requestedFrom,
            LocalDate requestedTo
    );

    Optional<BackfillJob> findJob(UUID backfillJobId);

    List<BackfillDate> findDates(UUID backfillJobId);

    /**
     * 오래 점유된 채 남은 기준일을 찾는다.
     *
     * 실행기가 강제 종료되면 기준일이 RUNNING으로 남는다. 점유 조회는 PENDING만 보므로
     * 그대로 두면 그 기준일은 다시 잡히지 않고 Job도 닫히지 않는다.
     *
     * {@code claimedBefore}보다 오래된 점유만 대상이다. 정상 실행 중인 다른 실행기의
     * 기준일을 뺏지 않도록 호출자가 한 기준일의 수집 소요보다 넉넉한 시각을 넘긴다.
     */
    List<BackfillDate> findStaleClaims(
            UUID backfillJobId,
            Instant claimedBefore
    );

    /**
     * 다음 PENDING 기준일을 원자적으로 점유한다.
     *
     * 같은 Job을 동시에 실행해도 두 실행기가 같은 기준일을 잡지 않는다.
     */
    Optional<BackfillDate> claimNextPendingDate(UUID backfillJobId);

    /**
     * 결과는 지금 점유한 실행기만 기록한다.
     *
     * {@code claimToken}은 점유할 때 받은 표다. 그 사이 점유가 회수되고 다른
     * 실행기가 다시 잡았으면 표가 달라 아무것도 바꾸지 않는다. 기준일 번호만 보면
     * 뒤늦게 도착한 결과가 새 점유를 덮어써 같은 기준일을 둘이 수집하고 시도
     * 한도도 무너진다.
     */
    void succeedDate(UUID backfillDateId, UUID claimToken);

    void skipDate(UUID backfillDateId, UUID claimToken);

    /**
     * 기준일 시도를 실패로 기록한다.
     *
     * {@code retryable}이면 다시 점유할 수 있도록 PENDING으로 되돌린다. 재시도 한도와
     * 간격 정책은 이 계약이 아니라 실행기가 정한다.
     */
    void failDate(
            UUID backfillDateId,
            UUID claimToken,
            BackfillErrorType errorType,
            boolean retryable
    );

    /**
     * 점유를 되돌린다.
     *
     * 시도로 세지 않는다. 이 기준일을 시험한 적이 없기 때문이다. 한도 초과처럼
     * 기준일과 무관한 사정으로 못 받았을 때 쓴다.
     *
     * 실패로 기록하면 점유할 때 오른 시도가 남는다. 그러면 그 사정이 반복될수록
     * 시도가 쌓이고, 나중에 진짜 일시 오류가 왔을 때 이미 소진돼 영구 실패한다.
     */
    void releaseDate(
            UUID backfillDateId,
            UUID claimToken,
            BackfillErrorType errorType
    );

    void cancelJob(UUID backfillJobId);

    record BackfillJob(
            UUID id,
            LocalDate requestedFrom,
            LocalDate requestedTo,
            BackfillStatus status,
            int succeededDateCount,
            int failedDateCount,
            int skippedDateCount,
            Instant createdAt,
            Instant startedAt,
            Instant finishedAt
    ) {
    }

    /**
     * {@code claimToken}은 이 기준일을 점유한 실행기를 가리키는 표다.
     *
     * 점유 중이 아니면 비어 있다. 결과를 기록할 때 이 값을 그대로 넘겨야 한다.
     */
    record BackfillDate(
            UUID id,
            UUID backfillJobId,
            LocalDate snapshotDate,
            BackfillStatus status,
            int attemptCount,
            BackfillErrorType lastErrorType,
            UUID claimToken,
            Instant startedAt,
            Instant finishedAt
    ) {
    }

    enum BackfillStatus {
        PENDING,
        RUNNING,
        SUCCEEDED,
        FAILED,
        SKIPPED,
        CANCELLED
    }

    /**
     * 오류 분류다.
     *
     * 자유 문자열을 두면 원본 응답 전문이나 Stack Trace가 흘러들어갈 수 있어 Enum으로
     * 제한한다. 상세 원인은 저장하지 않고 로그에서 다룬다.
     */
    enum BackfillErrorType {
        EXTERNAL_CLIENT,

        /** 한도를 넘겨 지금은 받을 수 없다. 요청 자체는 올바르다. */
        EXTERNAL_RATE_LIMITED,

        EXTERNAL_SERVER,
        EXTERNAL_TIMEOUT,
        RESPONSE_INVALID,
        STORE_FAILED,
        UNKNOWN
    }
}
