package com.maplemetric.character.domain.exception;

import com.maplemetric.global.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum CharacterErrorCode implements ErrorCode {

    CHARACTER_NOT_FOUND(HttpStatus.NOT_FOUND, "CHARACTER_001", "존재하지 않는 캐릭터입니다."),

    NEXON_API_CLIENT_ERROR(HttpStatus.BAD_GATEWAY, "CHARACTER_002", "넥슨 API 요청 처리 중 오류가 발생했습니다."),

    NEXON_API_SERVER_ERROR(HttpStatus.BAD_GATEWAY, "CHARACTER_003", "넥슨 API 서버와 통신 중 오류가 발생했습니다."),

    NEXON_API_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "CHARACTER_004", "넥슨 API 응답 시간이 초과되었습니다."),

    NEXON_API_RESPONSE_INVALID(HttpStatus.BAD_GATEWAY, "CHARACTER_005", "넥슨 API 응답 데이터가 올바르지 않습니다."),

    INVALID_CHARACTER_NAME(HttpStatus.BAD_REQUEST, "CHARACTER_006", "캐릭터명 형식이 올바르지 않습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    CharacterErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}