package com.maplemetric.statistics.presentation.controller;

import com.maplemetric.common.ApiResponse;
import com.maplemetric.ranking.api.OverallRankingWorldStatisticsQueryException;
import com.maplemetric.statistics.application.exception.JobStatisticsException;
import com.maplemetric.statistics.application.exception.WorldStatisticsException;
import com.maplemetric.statistics.presentation.code.StatisticsErrorCode;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = StatisticsController.class)
public class StatisticsExceptionHandler {

    @ExceptionHandler(JobStatisticsException.class)
    public ResponseEntity<ApiResponse<Void>> handleJobStatisticsException(
            JobStatisticsException exception
    ) {
        StatisticsErrorCode errorCode = resolveErrorCode(exception);

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.error(errorCode));
    }

    @ExceptionHandler(WorldStatisticsException.class)
    public ResponseEntity<ApiResponse<Void>> handleWorldStatisticsException(
            WorldStatisticsException exception
    ) {
        StatisticsErrorCode errorCode = resolveErrorCode(exception);

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.error(errorCode));
    }

    @ExceptionHandler(OverallRankingWorldStatisticsQueryException.class)
    public ResponseEntity<ApiResponse<Void>> handleOverallRankingWorldStatisticsQueryException(
            OverallRankingWorldStatisticsQueryException exception
    ) {
        StatisticsErrorCode errorCode = resolveErrorCode(exception);

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.error(errorCode));
    }

    private StatisticsErrorCode resolveErrorCode(
            JobStatisticsException exception
    ) {
        return switch (exception.getFailure()) {
            case SNAPSHOT_NOT_FOUND ->
                    StatisticsErrorCode.JOB_STATISTICS_SNAPSHOT_NOT_FOUND;
            case DATA_INVALID ->
                    StatisticsErrorCode.JOB_STATISTICS_DATA_INVALID;
            case JOB_NOT_FOUND ->
                    StatisticsErrorCode.JOB_STATISTICS_JOB_NOT_FOUND;
            case INVALID_HISTORY_REQUEST ->
                    StatisticsErrorCode
                            .JOB_STATISTICS_HISTORY_INVALID_REQUEST;
        };
    }

    private StatisticsErrorCode resolveErrorCode(
            WorldStatisticsException exception
    ) {
        return switch (exception.getFailure()) {
            case SNAPSHOT_NOT_FOUND ->
                    StatisticsErrorCode.WORLD_STATISTICS_SNAPSHOT_NOT_FOUND;
            case DATA_INVALID ->
                    StatisticsErrorCode.WORLD_STATISTICS_DATA_INVALID;
            case WORLD_NOT_FOUND ->
                    StatisticsErrorCode.WORLD_STATISTICS_WORLD_NOT_FOUND;
            case INVALID_HISTORY_REQUEST ->
                    StatisticsErrorCode
                            .WORLD_STATISTICS_HISTORY_INVALID_REQUEST;
        };
    }

    private StatisticsErrorCode resolveErrorCode(
            OverallRankingWorldStatisticsQueryException exception
    ) {
        return switch (exception.getFailure()) {
            case NOT_FOUND ->
                    StatisticsErrorCode.WORLD_STATISTICS_SNAPSHOT_NOT_FOUND;
            case DATA_INVALID ->
                    StatisticsErrorCode.WORLD_STATISTICS_DATA_INVALID;
        };
    }
}
