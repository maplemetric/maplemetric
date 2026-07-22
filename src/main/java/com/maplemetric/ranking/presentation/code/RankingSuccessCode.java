package com.maplemetric.ranking.presentation.code;

import com.maplemetric.common.SuccessCode;
import lombok.Getter;

@Getter
public enum RankingSuccessCode implements SuccessCode {

    OVERALL_RANKING_SEARCH_SUCCESS(
            "OVERALL_RANKING_SEARCH_SUCCESS",
            "종합 랭킹 조회에 성공했습니다."
    ),

    UNION_RANKING_SEARCH_SUCCESS(
            "UNION_RANKING_SEARCH_SUCCESS",
            "유니온 랭킹 조회에 성공했습니다."
    ),

    DOJANG_RANKING_SEARCH_SUCCESS(
            "DOJANG_RANKING_SEARCH_SUCCESS",
            "무릉도장 랭킹 조회에 성공했습니다."
    );

    private final String code;
    private final String message;

    RankingSuccessCode(
            String code,
            String message
    ) {
        this.code = code;
        this.message = message;
    }
}
