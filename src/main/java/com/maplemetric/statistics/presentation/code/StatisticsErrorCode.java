package com.maplemetric.statistics.presentation.code;

import com.maplemetric.common.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum StatisticsErrorCode implements ErrorCode {

    JOB_STATISTICS_SNAPSHOT_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "STATISTICS_001",
            "직업별 통계를 계산할 종합 랭킹 Snapshot이 없습니다."
    ),

    JOB_STATISTICS_DATA_INVALID(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "STATISTICS_002",
            "직업별 통계 집계 데이터가 정합하지 않습니다."
    ),

    WORLD_STATISTICS_SNAPSHOT_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "STATISTICS_003",
            "월드별 통계를 계산할 종합 랭킹 Snapshot이 없습니다."
    ),

    WORLD_STATISTICS_DATA_INVALID(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "STATISTICS_004",
            "월드별 통계 집계 데이터가 정합하지 않습니다."
    ),

    JOB_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "STATISTICS_005",
            "요청한 Canonical 직업 Slug를 찾을 수 없습니다."
    ),

    JOB_STATISTICS_HISTORY_INVALID_REQUEST(
            HttpStatus.BAD_REQUEST,
            "STATISTICS_006",
            "직업 통계 History 기간 요청이 올바르지 않습니다."
    );

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    StatisticsErrorCode(
            HttpStatus httpStatus,
            String code,
            String message
    ) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
