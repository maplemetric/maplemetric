package com.maplemetric.notice.domain.model;

import com.maplemetric.notice.domain.exception.InvalidNoticeCategoryException;
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
                        new InvalidNoticeCategoryException()
                );
    }
}
