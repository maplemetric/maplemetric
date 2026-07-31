package com.maplemetric.ranking.api;

import java.util.Objects;

public class OverallRankingWorldStatisticsHistoryQueryException
        extends RuntimeException {

    private final OverallRankingWorldStatisticsHistoryQueryFailure failure;

    public OverallRankingWorldStatisticsHistoryQueryException(
            OverallRankingWorldStatisticsHistoryQueryFailure failure
    ) {
        this.failure = Objects.requireNonNull(failure);
    }

    public OverallRankingWorldStatisticsHistoryQueryFailure getFailure() {
        return failure;
    }
}
