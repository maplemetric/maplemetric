package com.maplemetric.internal.presentation.code;

import com.maplemetric.common.SuccessCode;
import lombok.Getter;

@Getter
public enum InternalSuccessCode implements SuccessCode {

    OVERALL_RANKING_COLLECTION_SUCCESS(
            "OVERALL_RANKING_COLLECTION_SUCCESS",
            "종합 랭킹 Snapshot 수집에 성공했습니다."
    ),

    OVERALL_RANKING_COLLECTION_SKIPPED(
            "OVERALL_RANKING_COLLECTION_SKIPPED",
            "동일한 기준일과 조건의 종합 랭킹 Snapshot이 이미 존재합니다."
    );

    private final String code;
    private final String message;

    InternalSuccessCode(
            String code,
            String message
    ) {
        this.code = code;
        this.message = message;
    }
}
