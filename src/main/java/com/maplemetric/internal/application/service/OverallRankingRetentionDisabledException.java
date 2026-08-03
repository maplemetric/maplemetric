package com.maplemetric.internal.application.service;

/**
 * 만료가 비활성 상태에서 삭제를 요청했다.
 *
 * 보존 정책과 복구 수단이 확정되기 전까지 삭제 경로는 잠겨 있다.
 */
public class OverallRankingRetentionDisabledException
        extends RuntimeException {

    public OverallRankingRetentionDisabledException() {
        super("Overall Ranking 보존 만료가 비활성 상태입니다.");
    }
}
