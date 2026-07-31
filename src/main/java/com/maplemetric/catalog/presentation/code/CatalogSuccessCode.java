package com.maplemetric.catalog.presentation.code;

import com.maplemetric.common.SuccessCode;
import lombok.Getter;

@Getter
public enum CatalogSuccessCode implements SuccessCode {

    JOB_CATALOG_SEARCH_SUCCESS(
            "JOB_CATALOG_SEARCH_SUCCESS",
            "직업 기준정보 조회에 성공했습니다."
    ),

    WORLD_CATALOG_SEARCH_SUCCESS(
            "WORLD_CATALOG_SEARCH_SUCCESS",
            "월드 기준정보 조회에 성공했습니다."
    );

    private final String code;
    private final String message;

    CatalogSuccessCode(
            String code,
            String message
    ) {
        this.code = code;
        this.message = message;
    }
}
