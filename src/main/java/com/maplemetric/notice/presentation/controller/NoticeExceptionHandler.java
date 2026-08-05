package com.maplemetric.notice.presentation.controller;

import com.maplemetric.common.ApiResponse;
import com.maplemetric.notice.application.exception.NoticeException;
import com.maplemetric.notice.domain.exception.InvalidNoticeCategoryException;
import com.maplemetric.notice.presentation.code.NoticeErrorCode;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = NoticeController.class)
public class NoticeExceptionHandler {

    @ExceptionHandler(
            InvalidNoticeCategoryException.class
    )
    public ResponseEntity<ApiResponse<Void>> handleInvalidNoticeCategoryException() {
        return createErrorResponse(
                NoticeErrorCode.INVALID_CATEGORY
        );
    }

    @ExceptionHandler(NoticeException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoticeException(
            NoticeException exception
    ) {
        return createErrorResponse(
                NoticeErrorCode.from(
                        exception.getFailure()
                )
        );
    }

    private ResponseEntity<ApiResponse<Void>> createErrorResponse(
            NoticeErrorCode errorCode
    ) {
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.error(errorCode));
    }
}
