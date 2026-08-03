package com.maplemetric.internal.presentation.response;

/**
 * 한 배치 실행 결과다.
 *
 * {@code processedDateCount}가 상한과 같으면 남은 기준일이 있을 수 있다. 그때는 Job이
 * 끝날 때까지 다시 호출한다.
 */
public record RunOverallRankingBackfillHttpResponse(
        int processedDateCount,
        OverallRankingBackfillJobHttpResponse job
) {
}
