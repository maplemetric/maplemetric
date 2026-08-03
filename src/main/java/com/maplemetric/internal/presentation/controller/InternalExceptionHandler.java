package com.maplemetric.internal.presentation.controller;

import com.maplemetric.common.ApiResponse;
import com.maplemetric.internal.application.service.OverallRankingBackfillJobNotFoundException;
import com.maplemetric.internal.application.service.OverallRankingRetentionDisabledException;
import com.maplemetric.internal.presentation.code.InternalErrorCode;
import com.maplemetric.ranking.api.OverallRankingCollectionAlreadyRunningException;
import com.maplemetric.ranking.api.OverallRankingCollectionException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = {
        OverallRankingCollectionController.class,
        OverallRankingBackfillController.class,
        OverallRankingRetentionController.class
})
public class InternalExceptionHandler {

    @ExceptionHandler(OverallRankingBackfillJobNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleOverallRankingBackfillJobNotFoundException(
            OverallRankingBackfillJobNotFoundException exception
    ) {
        InternalErrorCode errorCode =
                InternalErrorCode.OVERALL_RANKING_BACKFILL_JOB_NOT_FOUND;

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.error(errorCode));
    }

    @ExceptionHandler(OverallRankingRetentionDisabledException.class)
    public ResponseEntity<ApiResponse<Void>> handleOverallRankingRetentionDisabledException(
            OverallRankingRetentionDisabledException exception
    ) {
        InternalErrorCode errorCode =
                InternalErrorCode.OVERALL_RANKING_RETENTION_DISABLED;

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.error(errorCode));
    }

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

    @ExceptionHandler(OverallRankingCollectionException.class)
    public ResponseEntity<ApiResponse<Void>> handleOverallRankingCollectionException(
            OverallRankingCollectionException exception
    ) {
        InternalErrorCode errorCode =
                InternalErrorCode.from(exception.getFailure());

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.error(errorCode));
    }
}
