package com.maplemetric.ranking.api;

import java.util.Objects;

public class OverallRankingComparisonQueryException extends RuntimeException {

    private final OverallRankingComparisonQueryFailure failure;

    public OverallRankingComparisonQueryException(
            OverallRankingComparisonQueryFailure failure
    ) {
        this.failure = Objects.requireNonNull(failure);
    }

    public OverallRankingComparisonQueryFailure getFailure() {
        return failure;
    }
}
