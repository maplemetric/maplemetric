package com.maplemetric.analysis.api;

/**
 * 한 번의 생성 실행 결과다.
 *
 * {@code generated}가 실행 한도와 같으면 남은 대상이 있다는 뜻이므로 다시 호출한다.
 *
 * {@code failed}는 대상별로 격리된 실패다. 한 대상이 실패해도 나머지는 계속 만든다.
 */
public record GenerateStatisticsInsightOutcome(
        int generated,
        int skipped,
        int failed
) {
}
