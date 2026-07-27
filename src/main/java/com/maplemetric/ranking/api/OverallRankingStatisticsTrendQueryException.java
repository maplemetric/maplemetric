package com.maplemetric.ranking.api;

import java.util.Objects;

public class OverallRankingStatisticsTrendQueryException extends RuntimeException {

    private final OverallRankingStatisticsTrendQueryFailure failure;

    public OverallRankingStatisticsTrendQueryException(
            OverallRankingStatisticsTrendQueryFailure failure
    ) {
        this.failure = Objects.requireNonNull(failure);
    }

    public OverallRankingStatisticsTrendQueryFailure getFailure() {
        return failure;
    }
}
