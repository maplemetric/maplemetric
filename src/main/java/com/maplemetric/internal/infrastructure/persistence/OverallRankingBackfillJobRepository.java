package com.maplemetric.internal.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface OverallRankingBackfillJobRepository
        extends JpaRepository<OverallRankingBackfillJobEntity, UUID> {

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
