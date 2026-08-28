package com.maplemetric.common.nexon;

/**
 * 넥슨 호출이 실패한 방식이다.
 *
 * 한도 초과를 따로 둔다. 일반 Client 오류로 뭉개면 요청 자체가 잘못된 것과
 * 구별되지 않아, 잠시 뒤면 성공할 요청이 영구 실패로 닫힌다.
 */
public enum NexonApiFailure {

    NOT_FOUND,

    /** 한도를 넘겨 지금은 받을 수 없다. 요청 자체는 올바르다. */
    RATE_LIMITED,

    CLIENT_ERROR,
    SERVER_ERROR,
    TIMEOUT,
    RESPONSE_INVALID
}
