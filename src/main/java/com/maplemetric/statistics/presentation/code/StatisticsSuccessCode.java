package com.maplemetric.statistics.presentation.code;

import com.maplemetric.common.SuccessCode;
import lombok.Getter;

@Getter
public enum StatisticsSuccessCode implements SuccessCode {

    JOB_STATISTICS_SEARCH_SUCCESS(
            "JOB_STATISTICS_SEARCH_SUCCESS",
            "직업별 통계 조회에 성공했습니다."
    );

    private final String code;
    private final String message;

    StatisticsSuccessCode(
            String code,
            String message
    ) {
        this.code = code;
        this.message = message;
    }
}
