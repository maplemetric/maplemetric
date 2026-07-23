package com.maplemetric.event.application.exception;

import java.util.Objects;

public class EventException extends RuntimeException {

    private final EventFailure failure;

    public EventException(EventFailure failure) {
        this.failure = Objects.requireNonNull(failure);
    }

    public EventFailure getFailure() {
        return failure;
    }
}
