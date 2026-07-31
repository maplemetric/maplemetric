package com.maplemetric.internal.infrastructure.persistence;

import com.maplemetric.internal.application.port.out.OverallRankingBackfillStatePort.BackfillStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface OverallRankingBackfillJobRepository
        extends JpaRepository<OverallRankingBackfillJobEntity, UUID> {

    /**
     * 종료 전이를 위해 Job 행을 잠근 채로 읽는다.
     *
     * Entity의 상태 확인만으로는 두 Transaction이 각자 읽은 옛 상태를 근거로 종료를
     * 쓸 수 있다. 취소한 Job이 SUCCEEDED로 덮이지 않게 행 잠금으로 순서를 만든다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select job
              from OverallRankingBackfillJobEntity job
             where job.id = :backfillJobId
            """)
    Optional<OverallRankingBackfillJobEntity> findByIdForUpdate(
            @Param("backfillJobId") UUID backfillJobId
    );

    /**
     * 첫 기준일을 점유할 때만 Job을 실행 중으로 바꾼다.
     *
     * 조건부 UPDATE라 Entity를 읽지 않는다. 점유마다 Job 행을 잠그면 서로 다른
     * 기준일을 잡는 실행기들이 Job 하나에 직렬화되므로 그렇게 하지 않는다.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update OverallRankingBackfillJobEntity job
               set job.status = :runningStatus,
                   job.startedAt = :startedAt
             where job.id = :backfillJobId
               and job.status = :pendingStatus
            """)
    int startIfPending(
            @Param("backfillJobId") UUID backfillJobId,
            @Param("runningStatus") BackfillStatus runningStatus,
            @Param("pendingStatus") BackfillStatus pendingStatus,
            @Param("startedAt") Instant startedAt
    );

    /**
     * 집계 컬럼을 DB에서 직접 증가시킨다.
     *
     * Entity를 읽어 더하면 같은 Job의 기준일을 동시에 끝낼 때 갱신이 유실된다.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update OverallRankingBackfillJobEntity job
               set job.succeededDateCount = job.succeededDateCount + 1
             where job.id = :backfillJobId
            """)
    int increaseSucceededDateCount(
            @Param("backfillJobId") UUID backfillJobId
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update OverallRankingBackfillJobEntity job
               set job.failedDateCount = job.failedDateCount + 1
             where job.id = :backfillJobId
            """)
    int increaseFailedDateCount(
            @Param("backfillJobId") UUID backfillJobId
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update OverallRankingBackfillJobEntity job
               set job.skippedDateCount = job.skippedDateCount + 1
             where job.id = :backfillJobId
            """)
    int increaseSkippedDateCount(
            @Param("backfillJobId") UUID backfillJobId
    );
}
