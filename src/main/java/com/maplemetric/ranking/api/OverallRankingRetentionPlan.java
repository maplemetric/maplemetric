package com.maplemetric.ranking.api;

import java.time.LocalDate;
import java.util.List;

/**
 * 만료 대상 산정 결과이자 실행 결과다.
 *
 * {@code retainedLatestDate}는 보존 기간과 무관하게 남기는 최신 기준일이다. 잘못된
 * 설정으로 마지막 남은 수집까지 지워 조회가 통째로 비는 것을 막는다.
 */
public record OverallRankingRetentionPlan(
        List<LocalDate> snapshotDates,
        long collectionCount,
        long snapshotCount,
        LocalDate retainedLatestDate
) {

    public static OverallRankingRetentionPlan empty(
            LocalDate retainedLatestDate
    ) {
        return new OverallRankingRetentionPlan(
                List.of(),
                0L,
                0L,
                retainedLatestDate
        );
    }

    public boolean isEmpty() {
        return snapshotDates.isEmpty();
    }
}
