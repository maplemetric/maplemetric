package com.maplemetric.internal.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface OverallRankingBackfillDateRepository
        extends JpaRepository<OverallRankingBackfillDateEntity, UUID> {

    List<OverallRankingBackfillDateEntity>
            findByBackfillJobIdOrderBySnapshotDateAsc(UUID backfillJobId);

    /**
     * 오래 점유된 채 남은 기준일을 찾는다.
     *
     * 실행기가 강제 종료되면 기준일이 RUNNING으로 남고, 점유 조회는 PENDING만 보므로
     * 그 기준일은 다시 잡히지 않는다. 임계 시각보다 오래된 점유만 대상으로 삼아
     * 정상 실행 중인 다른 실행기의 기준일을 뺏지 않는다.
     */
    @Query("""
            select date
              from OverallRankingBackfillDateEntity date
             where date.backfillJobId = :backfillJobId
               and date.status =
                   com.maplemetric.internal.application.port.out
                       .OverallRankingBackfillStatePort.BackfillStatus.RUNNING
               and date.startedAt < :claimedBefore
             order by date.snapshotDate
            """)
    List<OverallRankingBackfillDateEntity> findStaleClaims(
            @Param("backfillJobId") UUID backfillJobId,
            @Param("claimedBefore") Instant claimedBefore
    );

    /**
     * 결과를 기록하기 전에 기준일 행을 잠근 채로 읽는다.
     *
     * 잠그지 않으면 두 실행기가 같은 RUNNING 행을 각자 읽어 둘 다 상태 검사를
     * 통과하고, Job 집계가 중복으로 올라간다. 회수 경로는 잠금 없이 조회한 목록을
     * 별도 Transaction에서 처리하므로 그 사이에 다른 실행기가 상태를 바꿀 수 있다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select date
              from OverallRankingBackfillDateEntity date
             where date.id = :backfillDateId
            """)
    Optional<OverallRankingBackfillDateEntity> findByIdForUpdate(
            @Param("backfillDateId") UUID backfillDateId
    );

    /**
     * 취소처럼 여러 기준일을 한 번에 바꿀 때 행을 잠근 채로 읽는다.
     *
     * 잠그지 않으면 다른 실행기가 같은 기준일을 동시에 점유해 취소가 유실된다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select date
              from OverallRankingBackfillDateEntity date
             where date.backfillJobId = :backfillJobId
             order by date.snapshotDate
            """)
    List<OverallRankingBackfillDateEntity> findByBackfillJobIdForUpdate(
            @Param("backfillJobId") UUID backfillJobId
    );

    /**
     * 다음 PENDING 기준일을 잠근 채로 가져온다.
     *
     * {@code FOR UPDATE SKIP LOCKED}라 다른 실행기가 이미 잡은 행은 건너뛴다. 잠금을
     * 기다리지 않으므로 중복 실행기가 같은 기준일을 동시에 점유하지 못한다.
     */
    @Query(
            value = """
                    SELECT *
                      FROM p_overall_ranking_backfill_date
                     WHERE backfill_job_id = :backfillJobId
                       AND status = 'PENDING'
                     ORDER BY snapshot_date
                     LIMIT 1
                     FOR UPDATE SKIP LOCKED
                    """,
            nativeQuery = true
    )
    Optional<OverallRankingBackfillDateEntity> lockNextPending(
            @Param("backfillJobId") UUID backfillJobId
    );

    @Query("""
            select count(date)
              from OverallRankingBackfillDateEntity date
             where date.backfillJobId = :backfillJobId
               and date.status in (
                   com.maplemetric.internal.application.port.out
                       .OverallRankingBackfillStatePort.BackfillStatus.PENDING,
                   com.maplemetric.internal.application.port.out
                       .OverallRankingBackfillStatePort.BackfillStatus.RUNNING
               )
            """)
    long countUnfinished(@Param("backfillJobId") UUID backfillJobId);

    @Query("""
            select count(date)
              from OverallRankingBackfillDateEntity date
             where date.backfillJobId = :backfillJobId
               and date.status =
                   com.maplemetric.internal.application.port.out
                       .OverallRankingBackfillStatePort.BackfillStatus.FAILED
            """)
    long countFailed(@Param("backfillJobId") UUID backfillJobId);
}
