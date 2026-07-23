package com.maplemetric.event.domain.exception;

import com.maplemetric.common.BusinessException;

public class EventException extends BusinessException {

    public EventException(EventErrorCode errorCode) {
        super(errorCode);
    }
}
