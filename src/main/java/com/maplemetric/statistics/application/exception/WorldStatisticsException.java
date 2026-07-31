package com.maplemetric.statistics.application.exception;

import java.util.Objects;

public class WorldStatisticsException extends RuntimeException {

    private final WorldStatisticsFailure failure;

    public WorldStatisticsException(WorldStatisticsFailure failure) {
        this.failure = Objects.requireNonNull(failure);
    }

    public WorldStatisticsException(
            WorldStatisticsFailure failure,
            Throwable cause
    ) {
        super(cause);
        this.failure = Objects.requireNonNull(failure);
    }

    public WorldStatisticsFailure getFailure() {
        return failure;
    }
}
