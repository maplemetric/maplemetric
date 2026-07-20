package com.maplemetric.global.exception;

import com.maplemetric.global.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum GlobalErrorCode implements ErrorCode {

    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "GLOBAL_001", "요청값이 올바르지 않습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    GlobalErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}