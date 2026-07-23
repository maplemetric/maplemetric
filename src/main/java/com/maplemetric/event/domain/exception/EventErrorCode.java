package com.maplemetric.event.domain.exception;

import com.maplemetric.common.ErrorCode;
import com.maplemetric.common.nexon.NexonApiFailure;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum EventErrorCode implements ErrorCode {

    NEXON_API_CLIENT_ERROR(
            HttpStatus.BAD_GATEWAY,
            "EVENT_001",
            "넥슨 이벤트 API 요청 처리 중 오류가 발생했습니다.",
            NexonApiFailure.CLIENT_ERROR
    ),

    NEXON_API_SERVER_ERROR(
            HttpStatus.BAD_GATEWAY,
            "EVENT_002",
            "넥슨 이벤트 API 서버와 통신 중 오류가 발생했습니다.",
            NexonApiFailure.SERVER_ERROR
    ),

    NEXON_API_TIMEOUT(
            HttpStatus.GATEWAY_TIMEOUT,
            "EVENT_003",
            "넥슨 이벤트 API 응답 시간이 초과되었습니다.",
            NexonApiFailure.TIMEOUT
    ),

    NEXON_API_RESPONSE_INVALID(
            HttpStatus.BAD_GATEWAY,
            "EVENT_004",
            "넥슨 이벤트 API 응답 데이터가 올바르지 않습니다.",
            NexonApiFailure.RESPONSE_INVALID
    );

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
    private final NexonApiFailure failure;

    EventErrorCode(
            HttpStatus httpStatus,
            String code,
            String message,
            NexonApiFailure failure
    ) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
        this.failure = failure;
    }
}
