package com.maplemetric.internal.infrastructure.persistence;

import jakarta.persistence.LockModeType;
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
