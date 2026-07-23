package com.maplemetric.notice.application.exception;

import java.util.Objects;

public class NoticeException extends RuntimeException {

    private final NoticeFailure failure;

    public NoticeException(NoticeFailure failure) {
        this.failure = Objects.requireNonNull(failure);
    }

    public NoticeFailure getFailure() {
        return failure;
    }
}
