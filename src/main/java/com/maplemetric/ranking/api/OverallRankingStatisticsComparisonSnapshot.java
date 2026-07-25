package com.maplemetric.ranking.api;

import java.time.temporal.ChronoUnit;

public record OverallRankingStatisticsComparisonSnapshot(
        OverallRankingStatisticsSnapshot latest,
        OverallRankingStatisticsSnapshot previous,
        Integer daysBetween
) {

    public OverallRankingStatisticsComparisonSnapshot {
        if (latest == null) {
            throw new IllegalArgumentException(
                    "최신 종합 랭킹 통계는 비어 있을 수 없습니다."
            );
        }

        if (previous == null && daysBetween != null) {
            throw new IllegalArgumentException(
                    "이전 종합 랭킹 통계가 없으면 기준일 간격도 없어야 합니다."
            );
        }

        if (previous != null && daysBetween == null) {
            throw new IllegalArgumentException(
                    "이전 종합 랭킹 통계가 있으면 기준일 간격도 있어야 합니다."
            );
        }

        if (previous != null) {
            if (daysBetween < 1) {
                throw new IllegalArgumentException(
                        "기준일 간격은 1 이상이어야 합니다."
                );
            }

            long actualDaysBetween = ChronoUnit.DAYS.between(
                    previous.asOf(),
                    latest.asOf()
            );

            if (daysBetween != actualDaysBetween) {
                throw new IllegalArgumentException(
                        "기준일 간격이 최신과 이전 기준일 차이와 일치하지 않습니다."
                );
            }
        }
    }
}
