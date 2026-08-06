package com.maplemetric.analysis.infrastructure.openai;

import com.maplemetric.analysis.domain.model.StatisticsInsightFact;
import com.maplemetric.analysis.domain.model.StatisticsInsightFacts;
import com.maplemetric.statistics.api.StatisticsTrend;

/**
 * 설명이 말해야 할 방향을 하나로 정한다.
 *
 * Statistics가 판정한 Trend만 쓴다. 모델이 방향을 지어내지 못하도록 요청에 넣고,
 * 응답이 같은 값을 돌려줬는지 검증하는 기준으로도 쓴다.
 */
final class StatisticsInsightTrends {

    private StatisticsInsightTrends() {
    }

    static StatisticsTrend of(StatisticsInsightFacts facts) {
        return facts.facts()
                .stream()
                .map(StatisticsInsightFact::trend)
                .filter(trend -> trend != null)
                .findFirst()
                .orElse(StatisticsTrend.INSUFFICIENT_DATA);
    }
}
