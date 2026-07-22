package com.maplemetric.ranking.domain.exception;

import com.maplemetric.common.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum RankingErrorCode implements ErrorCode {

    NEXON_API_CLIENT_ERROR(
            HttpStatus.BAD_GATEWAY,
            "RANKING_001",
            "넥슨 랭킹 API 요청 처리 중 오류가 발생했습니다."
    ),

    NEXON_API_SERVER_ERROR(
            HttpStatus.BAD_GATEWAY,
            "RANKING_002",
            "넥슨 랭킹 API 서버와 통신 중 오류가 발생했습니다."
    ),

    NEXON_API_TIMEOUT(
            HttpStatus.GATEWAY_TIMEOUT,
            "RANKING_003",
            "넥슨 랭킹 API 응답 시간이 초과되었습니다."
    ),

    NEXON_API_RESPONSE_INVALID(
            HttpStatus.BAD_GATEWAY,
            "RANKING_004",
            "넥슨 랭킹 API 응답 데이터가 올바르지 않습니다."
    );

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    RankingErrorCode(
            HttpStatus httpStatus,
            String code,
            String message
    ) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
