package com.maplemetric.ranking.infrastructure.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OverallRankingCollectionJpaRepository
        extends JpaRepository<OverallRankingCollectionEntity, UUID> {

    boolean existsBySnapshotDateAndWorldNameAndWorldTypeAndClassName(
            LocalDate snapshotDate,
            String worldName,
            int worldType,
            String className
    );

    @Query("""
            select max(collection.snapshotDate)
              from OverallRankingCollectionEntity collection
            """)
    Optional<LocalDate> findLatestSnapshotDate();

    /**
     * 전체 조건으로 수집한 기준일을 기간 안에서 오름차순으로 찾는다.
     *
     * 월드나 직업을 좁혀 받은 수집이 있어도 그 기준일이 채워진 것은 아니다. 통계가
     * 보는 것은 전체 조건 저장본이라, 좁은 조건만 있는 날을 채워졌다고 세면 그 날의
     * 전체 조건 저장본은 영영 만들어지지 않는다.
     *
     * 비어 있는 기준일을 SQL로 만들어 내지 않는다. 날짜를 만들어 내려면 DB마다 다른
     * 함수를 써야 하고, 기간이 2년이라 그 목록도 크지 않다. 채워진 날을 받아 부르는
     * 쪽에서 빼는 편이 옮기기 쉽고 읽기도 쉽다.
     */
    @Query("""
            select distinct collection.snapshotDate
              from OverallRankingCollectionEntity collection
             where collection.snapshotDate between :from and :to
               and collection.worldName = :allWorldName
               and collection.worldType = :allWorldType
               and collection.className = :allClassName
             order by collection.snapshotDate
            """)
    List<LocalDate> findCollectedSnapshotDates(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("allWorldName") String allWorldName,
            @Param("allWorldType") int allWorldType,
            @Param("allClassName") String allClassName
    );

    /**
     * 전체 조건으로 수집한 가장 오래된 기준일이다.
     *
     * 이 날 이전은 수집을 시작하기 전이라 비어 있는 것이 정상이다. 그 기간까지 메우기
     * 대상으로 삼으면 한 번도 받은 적 없는 과거를 매일 받아 오게 되고, 정작 앱이 꺼져
     * 있어 빠진 최근 날은 뒤로 밀린다.
     */
    @Query("""
            select min(collection.snapshotDate)
              from OverallRankingCollectionEntity collection
             where collection.worldName = :allWorldName
               and collection.worldType = :allWorldType
               and collection.className = :allClassName
            """)
    Optional<LocalDate> findEarliestCollectedSnapshotDate(
            @Param("allWorldName") String allWorldName,
            @Param("allWorldType") int allWorldType,
            @Param("allClassName") String allClassName
    );

    /**
     * 만료 대상 기준일을 오래된 순으로 찾는다.
     *
     * 보존 기간이 지났어도 {@code retainedDate}는 제외한다. 보존 일수를 잘못 넣어도
     * 마지막 수집까지 지워 조회가 통째로 비는 일은 없어야 한다.
     */
    @Query("""
            select distinct collection.snapshotDate
              from OverallRankingCollectionEntity collection
             where collection.snapshotDate < :expireBefore
               and collection.snapshotDate <> :retainedDate
             order by collection.snapshotDate
            """)
    List<LocalDate> findExpirableSnapshotDates(
            @Param("expireBefore") LocalDate expireBefore,
            @Param("retainedDate") LocalDate retainedDate,
            Pageable pageable
    );

    @Query("""
            select count(collection)
              from OverallRankingCollectionEntity collection
             where collection.snapshotDate in :snapshotDates
            """)
    long countBySnapshotDates(
            @Param("snapshotDates") List<LocalDate> snapshotDates
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            delete from OverallRankingCollectionEntity collection
             where collection.snapshotDate in :snapshotDates
            """)
    int deleteBySnapshotDates(
            @Param("snapshotDates") List<LocalDate> snapshotDates
    );
}
