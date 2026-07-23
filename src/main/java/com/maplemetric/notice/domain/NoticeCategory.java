package com.maplemetric.notice.domain;

import com.maplemetric.notice.domain.exception.NoticeErrorCode;
import com.maplemetric.notice.domain.exception.NoticeException;
import java.util.Arrays;

public enum NoticeCategory {

    GENERAL("general"),
    UPDATE("update"),
    CASHSHOP("cashshop");

    private final String value;

    NoticeCategory(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static NoticeCategory from(String value) {
        return Arrays.stream(values())
                .filter(category ->
                        category.value.equalsIgnoreCase(value)
                )
                .findFirst()
                .orElseThrow(() ->
                        new NoticeException(
                                NoticeErrorCode.INVALID_CATEGORY
                        )
                );
    }
}
