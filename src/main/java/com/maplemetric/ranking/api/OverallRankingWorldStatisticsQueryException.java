package com.maplemetric.ranking.api;

import java.util.Objects;

public class OverallRankingWorldStatisticsQueryException extends RuntimeException {

    private final OverallRankingWorldStatisticsQueryFailure failure;

    public OverallRankingWorldStatisticsQueryException(
            OverallRankingWorldStatisticsQueryFailure failure
    ) {
        this.failure = Objects.requireNonNull(failure);
    }

    public OverallRankingWorldStatisticsQueryFailure getFailure() {
        return failure;
    }
}
