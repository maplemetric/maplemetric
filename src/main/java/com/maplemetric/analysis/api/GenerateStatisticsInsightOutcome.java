package com.maplemetric.analysis.api;

/**
 * 한 번의 생성 실행 결과다.
 *
 * {@code hasMore}가 참이면 실행 한도 때문에 멈춘 것이므로 다시 호출한다. 생성 건수를
 * 한도와 비교해 짐작하지 않는다. 정확히 마지막 대상까지 만들고 끝난 경우와 한도에
 * 걸려 멈춘 경우의 건수가 같기 때문이다.
 *
 * 건너뛴 이유를 나눠 센다. {@code alreadyExists}는 재실행이 비용을 늘리지 않는다는
 * 뜻이라 넘어가도 되지만, {@code noHistory}는 수집된 기준일이 없다는 뜻이라 확인이
 * 필요하다. 합쳐 세면 조치가 필요한 쪽을 놓친다.
 *
 * {@code failed}는 대상별로 격리된 실패다. 한 대상이 실패해도 나머지는 계속 만든다.
 */
public record GenerateStatisticsInsightOutcome(
        int generated,
        int alreadyExists,
        int noHistory,
        int failed,
        boolean hasMore
) {
}
