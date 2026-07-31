package com.maplemetric.ranking.api;

import java.util.Objects;

public class OverallRankingStatisticsHistoryQueryException extends RuntimeException {

    private final OverallRankingStatisticsHistoryQueryFailure failure;

    public OverallRankingStatisticsHistoryQueryException(
            OverallRankingStatisticsHistoryQueryFailure failure
    ) {
        this.failure = Objects.requireNonNull(failure);
    }

    public OverallRankingStatisticsHistoryQueryFailure getFailure() {
        return failure;
    }
}
