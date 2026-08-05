package com.maplemetric.ranking.presentation.controller;

import com.maplemetric.common.ApiResponse;
import com.maplemetric.ranking.domain.exception.RankingException;
import com.maplemetric.ranking.presentation.code.RankingErrorCode;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = RankingController.class)
public class RankingExceptionHandler {

    @ExceptionHandler(RankingException.class)
    public ResponseEntity<ApiResponse<Void>> handleRankingException(
            RankingException exception
    ) {
        RankingErrorCode errorCode =
                RankingErrorCode.from(exception.getFailure());

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.error(errorCode));
    }
}
