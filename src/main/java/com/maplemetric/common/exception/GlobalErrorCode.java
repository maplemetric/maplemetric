package com.maplemetric.common.exception;

import com.maplemetric.common.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum GlobalErrorCode implements ErrorCode {

    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "GLOBAL_001", "요청값이 올바르지 않습니다."),

    /**
     * 원인을 응답에 적지 않는다.
     *
     * 예외 메시지에는 SQL·제약 이름·내부 경로가 섞여 들어온다. 진단에 필요한 정보는
     * 로그에 남기고 응답에는 고정 문구만 내보낸다.
     */
    INTERNAL_SERVER_ERROR(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "GLOBAL_002",
            "요청을 처리하는 중 오류가 발생했습니다."
    );

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    GlobalErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}