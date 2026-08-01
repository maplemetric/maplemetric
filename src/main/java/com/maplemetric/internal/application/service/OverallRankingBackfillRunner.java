package com.maplemetric.internal.application.service;

import com.maplemetric.internal.application.port.out.OverallRankingBackfillStatePort.BackfillDate;
import com.maplemetric.internal.application.port.out.OverallRankingBackfillStatePort.BackfillErrorType;
import com.maplemetric.internal.application.port.out.OverallRankingBackfillStatePort.BackfillJob;
import com.maplemetric.internal.infrastructure.properties.OverallRankingBackfillProperties;
import com.maplemetric.ranking.api.CollectOverallRankingSnapshotRequest;
import com.maplemetric.ranking.api.CollectOverallRankingSnapshotUseCase;
import com.maplemetric.ranking.api.OverallRankingCollectionAlreadyRunningException;
import com.maplemetric.ranking.api.OverallRankingCollectionException;
import com.maplemetric.ranking.api.OverallRankingCollectionFailure;
import com.maplemetric.ranking.api.OverallRankingCollectionStatus;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 승인된 날짜 범위의 과거 Snapshot을 일자 단위로 수집한다.
 *
 * 수집 자체는 기존 Overall Ranking 수집 UseCase를 그대로 쓴다. 이미 같은 조건의
 * Collection이 있으면 그 UseCase가 SKIPPED를 돌려주므로 Backfill이 중복 저장 여부를
 * 따로 판단하지 않는다.
 *
 * Nexon 호출은 DB Transaction 밖에서 일어난다. 점유와 결과 기록은 각각 짧은
 * Transaction이고, 그 사이의 외부 호출은 어떤 Transaction에도 들어가지 않는다.
 *
 * 이 클래스는 스스로 실행되지 않는다. Scheduler도 Endpoint도 두지 않았으며, 실제 운영
 * Backfill은 사용자 승인 후 별도로 개시한다.
 */
@Slf4j
@Service
public class OverallRankingBackfillRunner {

    private final OverallRankingBackfillStateService backfillStateService;
    private final CollectOverallRankingSnapshotUseCase collectUseCase;
    private final OverallRankingBackfillProperties properties;
    private final Sleeper sleeper;

    @Autowired
    public OverallRankingBackfillRunner(
            OverallRankingBackfillStateService backfillStateService,
            CollectOverallRankingSnapshotUseCase collectUseCase,
            OverallRankingBackfillProperties properties
    ) {
        this(
                backfillStateService,
                collectUseCase,
                properties,
                duration -> Thread.sleep(duration.toMillis())
        );
    }

    OverallRankingBackfillRunner(
            OverallRankingBackfillStateService backfillStateService,
            CollectOverallRankingSnapshotUseCase collectUseCase,
            OverallRankingBackfillProperties properties,
            Sleeper sleeper
    ) {
        this.backfillStateService = backfillStateService;
        this.collectUseCase = collectUseCase;
        this.properties = properties;
        this.sleeper = sleeper;
    }

    public BackfillJob createJob(
            LocalDate requestedFrom,
            LocalDate requestedTo
    ) {
        BackfillJob job = backfillStateService.createJob(
                requestedFrom,
                requestedTo
        );

        log.info(
                "Backfill Job을 생성했습니다. jobId={}, 기간={}~{}",
                job.id(),
                job.requestedFrom(),
                job.requestedTo()
        );

        return job;
    }

    /**
     * 남은 기준일을 순서대로 처리한다.
     *
     * 한 번의 실행은 {@code maxDatesPerRun}개까지만 처리하고 멈춘다. 남은 기준일은
     * 상태에 그대로 남아 다음 실행이 이어받는다.
     *
     * @return 이번 실행에서 처리한 기준일 수
     */
    public int run(UUID backfillJobId) {
        reclaimStaleClaims(backfillJobId);

        int processed = 0;

        while (processed < properties.maxDatesPerRun()) {
            Optional<BackfillDate> claimed =
                    backfillStateService.claimNextPendingDate(backfillJobId);

            if (claimed.isEmpty()) {
                break;
            }

            BackfillDate date = claimed.get();

            // 빈 대기열에서 헛되이 기다리지 않도록 점유 뒤에 간격을 둔다.
            if (processed > 0) {
                sleepBetweenRequests(date);
            }

            processDate(date);
            processed++;
        }

        log.info(
                "Backfill 실행을 마쳤습니다. jobId={}, 처리한 기준일 수={}",
                backfillJobId,
                processed
        );

        return processed;
    }

    /**
     * 앞선 실행이 점유한 채 남긴 기준일을 회수한다.
     *
     * 실행기가 강제 종료되면 기준일이 RUNNING으로 남는다. 점유 조회는 PENDING만 보므로
     * 회수하지 않으면 그 기준일은 영영 다시 잡히지 않고 Job도 닫히지 않는다.
     *
     * 회수는 실패 기록과 같은 경로를 쓴다. 그래야 시도 한도 규칙이 그대로 적용돼
     * 매번 죽는 기준일이 무한히 회수되지 않는다.
     */
    private void reclaimStaleClaims(UUID backfillJobId) {
        Instant claimedBefore =
                Instant.now().minus(properties.staleClaimTimeout());

        List<BackfillDate> staleClaims =
                backfillStateService.findStaleClaims(
                        backfillJobId,
                        claimedBefore
                );

        if (staleClaims.isEmpty()) {
            return;
        }

        log.warn(
                "중단된 점유를 회수합니다. jobId={}, 기준일 수={}",
                backfillJobId,
                staleClaims.size()
        );

        staleClaims.forEach(date -> recordFailure(
                date,
                BackfillErrorType.UNKNOWN,
                true
        ));
    }

    private void processDate(BackfillDate date) {
        try {
            OverallRankingCollectionStatus status = collectUseCase.collect(
                    new CollectOverallRankingSnapshotRequest(
                            date.snapshotDate(),
                            properties.maxPages()
                    )
            ).status();

            if (status == OverallRankingCollectionStatus.SKIPPED) {
                log.info(
                        "이미 수집된 기준일이라 건너뜁니다. 기준일={}",
                        date.snapshotDate()
                );

                backfillStateService.skipDate(date.id());
                return;
            }

            backfillStateService.succeedDate(date.id());
        } catch (OverallRankingCollectionAlreadyRunningException exception) {
            // 정기 수집과 겹친 상황이다. 이 기준일 자체의 문제가 아니므로 다시 시도한다.
            recordFailure(date, BackfillErrorType.EXTERNAL_SERVER, true);
        } catch (OverallRankingCollectionException exception) {
            recordFailure(
                    date,
                    toErrorType(exception.getFailure()),
                    isRetryable(exception.getFailure())
            );
        } catch (RuntimeException exception) {
            log.error(
                    "Backfill 기준일 처리에 실패했습니다. 기준일={}",
                    date.snapshotDate(),
                    exception
            );

            recordFailure(date, BackfillErrorType.STORE_FAILED, true);
        }
    }

    /**
     * 실패를 기록한다.
     *
     * 재시도 가능한 오류라도 시도 횟수가 한도에 닿으면 더 돌리지 않는다. 한도가 없으면
     * 같은 기준일을 무한히 다시 잡는다.
     */
    private void recordFailure(
            BackfillDate date,
            BackfillErrorType errorType,
            boolean retryable
    ) {
        boolean retryLeft = retryable
                && date.attemptCount() < properties.maxAttemptsPerDate();

        log.warn(
                "Backfill 기준일 수집에 실패했습니다. "
                        + "기준일={}, 오류={}, 시도={}/{}, 재시도={}",
                date.snapshotDate(),
                errorType,
                date.attemptCount(),
                properties.maxAttemptsPerDate(),
                retryLeft
        );

        backfillStateService.failDate(date.id(), errorType, retryLeft);
    }

    /**
     * 다음 호출까지 간격을 둔다.
     *
     * 이 시점에는 이미 기준일을 점유한 상태다. 중단되면 그 기준일을 RUNNING으로 남긴 채
     * 실행이 끝나는데, 점유를 회수하는 경로가 없어 그 기준일은 다시 잡히지 않는다.
     * 중단 시 점유를 먼저 풀고 예외를 올린다.
     */
    private void sleepBetweenRequests(BackfillDate claimedDate) {
        if (properties.requestInterval().isZero()) {
            return;
        }

        try {
            sleeper.sleep(properties.requestInterval());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            recordFailure(claimedDate, BackfillErrorType.UNKNOWN, true);

            throw new IllegalStateException(
                    "Backfill 실행이 중단되었습니다.",
                    exception
            );
        }
    }

    private BackfillErrorType toErrorType(
            OverallRankingCollectionFailure failure
    ) {
        return switch (failure) {
            case EXTERNAL_API_CLIENT_ERROR ->
                    BackfillErrorType.EXTERNAL_CLIENT;
            case EXTERNAL_API_SERVER_ERROR ->
                    BackfillErrorType.EXTERNAL_SERVER;
            case EXTERNAL_API_TIMEOUT -> BackfillErrorType.EXTERNAL_TIMEOUT;
            case EXTERNAL_API_RESPONSE_INVALID ->
                    BackfillErrorType.RESPONSE_INVALID;
        };
    }

    /**
     * 일시 오류와 영구 오류를 구분한다.
     *
     * 응답 검증 실패는 다시 불러도 같은 응답이 오므로 재시도하지 않는다. Client 오류도
     * 요청 자체가 잘못된 것이라 반복해봐야 Quota만 쓴다.
     */
    private boolean isRetryable(OverallRankingCollectionFailure failure) {
        return switch (failure) {
            case EXTERNAL_API_SERVER_ERROR, EXTERNAL_API_TIMEOUT -> true;
            case EXTERNAL_API_CLIENT_ERROR, EXTERNAL_API_RESPONSE_INVALID ->
                    false;
        };
    }

    /**
     * 호출 간격을 두는 수단이다.
     *
     * 테스트에서 실제로 기다리지 않기 위해 분리한다.
     */
    interface Sleeper {

        void sleep(Duration duration) throws InterruptedException;
    }
}
