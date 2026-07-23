package com.maplemetric.event.presentation.code;

import com.maplemetric.common.ErrorCode;
import com.maplemetric.event.application.exception.EventFailure;
import java.util.Objects;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum EventErrorCode implements ErrorCode {

    NEXON_API_CLIENT_ERROR(
            HttpStatus.BAD_GATEWAY,
            "EVENT_001",
            "넥슨 이벤트 API 요청 처리 중 오류가 발생했습니다.",
            EventFailure.CLIENT_ERROR
    ),

    NEXON_API_SERVER_ERROR(
            HttpStatus.BAD_GATEWAY,
            "EVENT_002",
            "넥슨 이벤트 API 서버와 통신 중 오류가 발생했습니다.",
            EventFailure.SERVER_ERROR
    ),

    NEXON_API_TIMEOUT(
            HttpStatus.GATEWAY_TIMEOUT,
            "EVENT_003",
            "넥슨 이벤트 API 응답 시간이 초과되었습니다.",
            EventFailure.TIMEOUT
    ),

    NEXON_API_RESPONSE_INVALID(
            HttpStatus.BAD_GATEWAY,
            "EVENT_004",
            "넥슨 이벤트 API 응답 데이터가 올바르지 않습니다.",
            EventFailure.RESPONSE_INVALID
    );

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
    private final EventFailure failure;

    EventErrorCode(
            HttpStatus httpStatus,
            String code,
            String message,
            EventFailure failure
    ) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
        this.failure = failure;
    }

    public static EventErrorCode from(
            EventFailure failure
    ) {
        Objects.requireNonNull(failure);

        return switch (failure) {
            case CLIENT_ERROR ->
                    NEXON_API_CLIENT_ERROR;
            case SERVER_ERROR ->
                    NEXON_API_SERVER_ERROR;
            case TIMEOUT ->
                    NEXON_API_TIMEOUT;
            case RESPONSE_INVALID ->
                    NEXON_API_RESPONSE_INVALID;
        };
    }
}
