package com.maplemetric.event.presentation.code;

import com.maplemetric.common.SuccessCode;
import lombok.Getter;

@Getter
public enum EventSuccessCode implements SuccessCode {

    ONGOING_EVENT_SEARCH_SUCCESS(
            "ONGOING_EVENT_SEARCH_SUCCESS",
            "진행 중 이벤트 목록 조회에 성공했습니다."
    );

    private final String code;
    private final String message;

    EventSuccessCode(
            String code,
            String message
    ) {
        this.code = code;
        this.message = message;
    }
}
