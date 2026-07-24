package com.maplemetric.internal.presentation.controller;

import com.maplemetric.common.ApiResponse;
import com.maplemetric.internal.presentation.code.InternalErrorCode;
import com.maplemetric.ranking.api.OverallRankingCollectionAlreadyRunningException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = OverallRankingCollectionController.class)
public class InternalExceptionHandler {

    @ExceptionHandler(OverallRankingCollectionAlreadyRunningException.class)
    public ResponseEntity<ApiResponse<Void>> handleOverallRankingCollectionAlreadyRunningException(
            OverallRankingCollectionAlreadyRunningException exception
    ) {
        InternalErrorCode errorCode =
                InternalErrorCode.OVERALL_RANKING_COLLECTION_ALREADY_RUNNING;

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.error(errorCode));
    }
}
