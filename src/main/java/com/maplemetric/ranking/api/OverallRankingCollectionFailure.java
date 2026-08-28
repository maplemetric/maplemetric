package com.maplemetric.ranking.api;

/**
 * 종합 랭킹 수집이 실패한 방식이다.
 *
 * 한도 초과를 따로 둔다. Client 오류로 뭉개면 요청 자체가 잘못된 것과 구별되지
 * 않아, 잠시 뒤면 성공할 기준일이 영구 실패로 닫힌다.
 */
public enum OverallRankingCollectionFailure {

    EXTERNAL_API_CLIENT_ERROR,

    /** 한도를 넘겨 지금은 받을 수 없다. 요청 자체는 올바르다. */
    EXTERNAL_API_RATE_LIMITED,

    EXTERNAL_API_SERVER_ERROR,
    EXTERNAL_API_TIMEOUT,
    EXTERNAL_API_RESPONSE_INVALID
}
