package com.maplemetric.notice.presentation.code;

import com.maplemetric.common.SuccessCode;
import lombok.Getter;

@Getter
public enum NoticeSuccessCode implements SuccessCode {

    NOTICE_SEARCH_SUCCESS(
            "NOTICE_SEARCH_SUCCESS",
            "공지 목록 조회에 성공했습니다."
    );

    private final String code;
    private final String message;

    NoticeSuccessCode(
            String code,
            String message
    ) {
        this.code = code;
        this.message = message;
    }
}
