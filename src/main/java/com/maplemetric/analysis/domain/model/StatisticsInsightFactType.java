package com.maplemetric.analysis.domain.model;

/**
 * 통계 InsightFact의 종류이자 우선순위다.
 *
 * 선언 순서가 곧 우선순위다. 별도 우선순위 필드를 두면 값과 순서가 어긋날 수 있고,
 * 같은 종류가 두 번 들어오면 앞선 것만 남긴다.
 *
 * 데이터가 부족하다는 사실이 가장 앞이다. 그것을 말하지 않고 수치부터 말하면 근거가
 * 없는 수치를 앞세우게 된다.
 */
public enum StatisticsInsightFactType {
    INSUFFICIENT_DATA,
    SHARE_CHANGE,
    COUNT_CHANGE,
    SHARE,
    COUNT,
    AVERAGE_LEVEL
}
