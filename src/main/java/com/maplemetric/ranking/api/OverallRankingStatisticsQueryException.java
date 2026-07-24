package com.maplemetric.ranking.api;

import java.util.Objects;

public class OverallRankingStatisticsQueryException extends RuntimeException {

    private final OverallRankingStatisticsQueryFailure failure;

    public OverallRankingStatisticsQueryException(
            OverallRankingStatisticsQueryFailure failure
    ) {
        this.failure = Objects.requireNonNull(failure);
    }

    public OverallRankingStatisticsQueryFailure getFailure() {
        return failure;
    }
}
