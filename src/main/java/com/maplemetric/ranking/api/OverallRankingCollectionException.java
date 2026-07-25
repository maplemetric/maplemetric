package com.maplemetric.ranking.api;

import java.util.Objects;

public class OverallRankingCollectionException extends RuntimeException {

    private final OverallRankingCollectionFailure failure;

    public OverallRankingCollectionException(
            OverallRankingCollectionFailure failure
    ) {
        this.failure = Objects.requireNonNull(failure);
    }

    public OverallRankingCollectionFailure getFailure() {
        return failure;
    }
}
