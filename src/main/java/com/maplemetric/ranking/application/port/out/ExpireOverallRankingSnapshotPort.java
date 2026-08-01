package com.maplemetric.ranking.application.port.out;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExpireOverallRankingSnapshotPort {

    Optional<LocalDate> findLatestSnapshotDate();

    /**
     * 만료 대상 기준일을 오래된 순으로 찾는다.
     *
     * @param retainedDate 보존 기간과 무관하게 제외할 기준일
     */
    List<LocalDate> findExpirableSnapshotDates(
            LocalDate expireBefore,
            LocalDate retainedDate,
            int limit
    );

    long countCollections(List<LocalDate> snapshotDates);

    long countSnapshots(List<LocalDate> snapshotDates);

    /**
     * 대상 기준일의 Snapshot과 Collection을 지운다.
     *
     * @return 삭제한 Collection 수와 Snapshot 수
     */
    DeletedCounts deleteBySnapshotDates(List<LocalDate> snapshotDates);

    record DeletedCounts(
            long collectionCount,
            long snapshotCount
    ) {
    }
}
