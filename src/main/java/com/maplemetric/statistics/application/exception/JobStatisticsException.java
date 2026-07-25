package com.maplemetric.statistics.application.exception;

import java.util.Objects;

public class JobStatisticsException extends RuntimeException {

    private final JobStatisticsFailure failure;

    public JobStatisticsException(
            JobStatisticsFailure failure,
            Throwable cause
    ) {
        super(cause);
        this.failure = Objects.requireNonNull(failure);
    }

    public JobStatisticsFailure getFailure() {
        return failure;
    }
}
