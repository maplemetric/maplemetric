package com.maplemetric.ranking.api;

import java.time.LocalDate;

/**
 * 만료 대상 조건이다.
 *
 * {@code expireBefore} 이전 기준일이 대상이고 그날 자체는 남는다.
 *
 * {@code maxDatesPerRun}은 한 번에 지우는 기준일 수를 묶는다. 오래 잠긴 대량 삭제
 * Transaction이 수집·조회와 겹치지 않게 한다.
 */
public record OverallRankingRetentionRequest(
        LocalDate expireBefore,
        int maxDatesPerRun
) {

    public OverallRankingRetentionRequest {
        if (expireBefore == null) {
            throw new IllegalArgumentException(
                    "만료 기준일은 비어 있을 수 없습니다."
            );
        }

        if (maxDatesPerRun < 1) {
            throw new IllegalArgumentException(
                    "한 번에 만료할 기준일 수는 1 이상이어야 합니다."
            );
        }
    }
}
