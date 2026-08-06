package com.maplemetric.statistics.application.result;

import java.util.List;

/**
 * 이 수치로 말할 수 없는 것을 적는다.
 *
 * 직업과 월드가 같은 문구를 써야 소비 측이 대상에 따라 다르게 해석하지 않는다.
 * 누락 일수는 Range의 {@code missingDateCount}가 이미 수로 제공하므로 문장으로
 * 다시 적지 않는다.
 */
final class StatisticsLimitations {

    static final List<String> HISTORY = List.of(
            "전체 이용자 모집단이 아니라 수집한 종합 랭킹 표본입니다.",
            "누락 날짜는 0으로 보간하지 않았습니다."
    );

    private StatisticsLimitations() {
    }
}
