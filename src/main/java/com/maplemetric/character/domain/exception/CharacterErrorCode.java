package com.maplemetric.character.domain.exception;

import com.maplemetric.global.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum CharacterErrorCode implements ErrorCode {

    CHARACTER_NOT_FOUND(HttpStatus.NOT_FOUND,"CHARACTER_001","존재하지 않는 캐릭터입니다."),
    CHARACTER_API_ERROR(HttpStatus.BAD_GATEWAY, "CHARACTER_002", "넥슨 API 호출 중 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    CharacterErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
