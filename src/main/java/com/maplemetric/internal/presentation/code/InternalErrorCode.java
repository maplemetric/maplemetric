package com.maplemetric.internal.presentation.code;

import com.maplemetric.common.ErrorCode;
import com.maplemetric.ranking.api.OverallRankingCollectionFailure;
import java.util.Objects;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum InternalErrorCode implements ErrorCode {

    INTERNAL_ACCESS_DENIED(
            HttpStatus.UNAUTHORIZED,
            "INTERNAL_001",
            "내부 API 인증에 실패했습니다."
    ),

    OVERALL_RANKING_COLLECTION_ALREADY_RUNNING(
            HttpStatus.CONFLICT,
            "INTERNAL_002",
            "종합 랭킹 Snapshot 수집이 이미 실행 중입니다."
    ),

    OVERALL_RANKING_COLLECTION_EXTERNAL_CLIENT_ERROR(
            HttpStatus.BAD_GATEWAY,
            "INTERNAL_003",
            "넥슨 랭킹 API 요청 처리 중 오류가 발생했습니다."
    ),

    OVERALL_RANKING_COLLECTION_EXTERNAL_SERVER_ERROR(
            HttpStatus.BAD_GATEWAY,
            "INTERNAL_004",
            "넥슨 랭킹 API 서버와 통신 중 오류가 발생했습니다."
    ),

    OVERALL_RANKING_COLLECTION_EXTERNAL_TIMEOUT(
            HttpStatus.GATEWAY_TIMEOUT,
            "INTERNAL_005",
            "넥슨 랭킹 API 응답 시간이 초과되었습니다."
    ),

    OVERALL_RANKING_COLLECTION_EXTERNAL_RESPONSE_INVALID(
            HttpStatus.BAD_GATEWAY,
            "INTERNAL_006",
            "넥슨 랭킹 API 응답 데이터가 올바르지 않습니다."
    );

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    InternalErrorCode(
            HttpStatus httpStatus,
            String code,
            String message
    ) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }

    public static InternalErrorCode from(
            OverallRankingCollectionFailure failure
    ) {
        Objects.requireNonNull(failure);

        return switch (failure) {
            case EXTERNAL_API_CLIENT_ERROR ->
                    OVERALL_RANKING_COLLECTION_EXTERNAL_CLIENT_ERROR;
            case EXTERNAL_API_SERVER_ERROR ->
                    OVERALL_RANKING_COLLECTION_EXTERNAL_SERVER_ERROR;
            case EXTERNAL_API_TIMEOUT ->
                    OVERALL_RANKING_COLLECTION_EXTERNAL_TIMEOUT;
            case EXTERNAL_API_RESPONSE_INVALID ->
                    OVERALL_RANKING_COLLECTION_EXTERNAL_RESPONSE_INVALID;
        };
    }
}
