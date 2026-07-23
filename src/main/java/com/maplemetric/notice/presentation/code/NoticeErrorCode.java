package com.maplemetric.notice.presentation.code;

import com.maplemetric.common.ErrorCode;
import com.maplemetric.notice.application.exception.NoticeFailure;
import java.util.Objects;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum NoticeErrorCode implements ErrorCode {

    INVALID_CATEGORY(
            HttpStatus.BAD_REQUEST,
            "NOTICE_001",
            "공지 분류가 올바르지 않습니다."
    ),

    NEXON_API_CLIENT_ERROR(
            HttpStatus.BAD_GATEWAY,
            "NOTICE_002",
            "넥슨 공지 API 요청 처리 중 오류가 발생했습니다."
    ),

    NEXON_API_SERVER_ERROR(
            HttpStatus.BAD_GATEWAY,
            "NOTICE_003",
            "넥슨 공지 API 서버와 통신 중 오류가 발생했습니다."
    ),

    NEXON_API_TIMEOUT(
            HttpStatus.GATEWAY_TIMEOUT,
            "NOTICE_004",
            "넥슨 공지 API 응답 시간이 초과되었습니다."
    ),

    NEXON_API_RESPONSE_INVALID(
            HttpStatus.BAD_GATEWAY,
            "NOTICE_005",
            "넥슨 공지 API 응답 데이터가 올바르지 않습니다."
    );

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    NoticeErrorCode(
            HttpStatus httpStatus,
            String code,
            String message
    ) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }

    public static NoticeErrorCode from(
            NoticeFailure failure
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
