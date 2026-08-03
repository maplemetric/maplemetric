package com.maplemetric.internal.presentation.code;

import com.maplemetric.common.SuccessCode;
import lombok.Getter;

@Getter
public enum InternalSuccessCode implements SuccessCode {

    OVERALL_RANKING_COLLECTION_SUCCESS(
            "OVERALL_RANKING_COLLECTION_SUCCESS",
            "종합 랭킹 Snapshot 수집에 성공했습니다."
    ),

    OVERALL_RANKING_COLLECTION_SKIPPED(
            "OVERALL_RANKING_COLLECTION_SKIPPED",
            "동일한 기준일과 조건의 종합 랭킹 Snapshot이 이미 존재합니다."
    ),

    OVERALL_RANKING_BACKFILL_JOB_CREATED(
            "OVERALL_RANKING_BACKFILL_JOB_CREATED",
            "종합 랭킹 Backfill Job을 생성했습니다."
    ),

    OVERALL_RANKING_BACKFILL_JOB_FOUND(
            "OVERALL_RANKING_BACKFILL_JOB_FOUND",
            "종합 랭킹 Backfill Job 상태를 조회했습니다."
    ),

    OVERALL_RANKING_BACKFILL_RUN_SUCCESS(
            "OVERALL_RANKING_BACKFILL_RUN_SUCCESS",
            "종합 랭킹 Backfill을 실행했습니다."
    ),

    OVERALL_RANKING_RETENTION_PLANNED(
            "OVERALL_RANKING_RETENTION_PLANNED",
            "종합 랭킹 수집 결과 만료 대상을 산정했습니다."
    ),

    OVERALL_RANKING_RETENTION_EXPIRED(
            "OVERALL_RANKING_RETENTION_EXPIRED",
            "종합 랭킹 수집 결과를 만료시켰습니다."
    );

    private final String code;
    private final String message;

    InternalSuccessCode(
            String code,
            String message
    ) {
        this.code = code;
        this.message = message;
    }
}
