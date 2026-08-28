package com.maplemetric.ranking.api;

import java.time.LocalDate;

public record CollectOverallRankingSnapshotRequest(
        LocalDate rankingDate,
        int maxPages,
        OverallRankingCollectionRequestClass requestClass
) {

    public CollectOverallRankingSnapshotRequest {
        if (requestClass == null) {
            throw new IllegalArgumentException(
                    "수집 요청 등급은 필수입니다."
            );
        }
    }

    /**
     * 실패하면 안 되는 수집으로 요청한다.
     *
     * 등급을 밝히지 않은 호출은 지금까지와 같은 예산을 쓴다. 대량 수집만 따로
     * 밝히게 해서, 기존 호출이 모르는 사이에 예산을 옮겨 가지 않게 한다.
     */
    public CollectOverallRankingSnapshotRequest(
            LocalDate rankingDate,
            int maxPages
    ) {
        this(
                rankingDate,
                maxPages,
                OverallRankingCollectionRequestClass.CRITICAL
        );
    }
}
