package com.maplemetric.internal.presentation.code;

import com.maplemetric.common.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum InternalErrorCode implements ErrorCode {

    INTERNAL_ACCESS_DENIED(
            HttpStatus.UNAUTHORIZED,
            "INTERNAL_001",
            "내부 API 인증에 실패했습니다."
    ),

    OVERALL_RANKING_COLLECTION_ALREADY_RUNNING(
            HttpStatus.CONFLICT,
            "INTERNAL_002",
            "종합 랭킹 Snapshot 수집이 이미 실행 중입니다."
    );

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    InternalErrorCode(
            HttpStatus httpStatus,
            String code,
            String message
    ) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
