package com.maplemetric.ranking.api;

/**
 * 보존 기간이 지난 Overall Ranking 수집 결과를 만료시킨다.
 *
 * 삭제는 되돌릴 수 없으므로 대상 산정과 실행을 분리한다. {@code plan}은 아무것도
 * 바꾸지 않고 대상만 계산하며, 실행 전에 무엇이 지워질지 확인하는 용도다.
 */
public interface ExpireOverallRankingSnapshotUseCase {

    OverallRankingRetentionPlan plan(OverallRankingRetentionRequest request);

    /**
     * 산정된 대상을 실제로 삭제한다.
     *
     * @return 삭제한 기준일과 행 수. 산정 시점과 실행 시점 사이에 데이터가 늘어날 수
     *         있으므로 {@code plan}의 결과와 항상 같지는 않다.
     */
    OverallRankingRetentionPlan expire(OverallRankingRetentionRequest request);
}
