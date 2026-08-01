package com.maplemetric.ranking.infrastructure.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OverallRankingSnapshotJpaRepository
        extends JpaRepository<OverallRankingSnapshotEntity, UUID> {

    List<OverallRankingSnapshotEntity> findByCollectionIdOrderByRankingAsc(
            UUID collectionId
    );

    @Query("""
            select count(snapshot)
              from OverallRankingSnapshotEntity snapshot
             where snapshot.collection.snapshotDate in :snapshotDates
            """)
    long countBySnapshotDates(
            @Param("snapshotDates") List<LocalDate> snapshotDates
    );

    /**
     * 대상 기준일의 Snapshot을 지운다.
     *
     * Collection보다 먼저 지워야 참조 무결성이 깨지지 않는다.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            delete from OverallRankingSnapshotEntity snapshot
             where snapshot.collection.id in (
                   select collection.id
                     from OverallRankingCollectionEntity collection
                    where collection.snapshotDate in :snapshotDates
               )
            """)
    int deleteBySnapshotDates(
            @Param("snapshotDates") List<LocalDate> snapshotDates
    );
}
