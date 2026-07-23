package com.maplemetric.event.presentation.controller;

import com.maplemetric.common.ApiResponse;
import com.maplemetric.event.application.exception.EventException;
import com.maplemetric.event.presentation.code.EventErrorCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = EventController.class)
public class EventExceptionHandler {

    @ExceptionHandler(EventException.class)
    public ResponseEntity<ApiResponse<Void>> handleEventException(
            EventException exception
    ) {
        EventErrorCode errorCode =
                EventErrorCode.from(exception.getFailure());

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.error(errorCode));
    }
}
