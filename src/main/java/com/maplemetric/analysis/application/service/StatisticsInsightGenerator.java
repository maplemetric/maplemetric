package com.maplemetric.analysis.application.service;

import com.maplemetric.analysis.application.result.StatisticsInsightPreview;
import com.maplemetric.analysis.domain.model.StatisticsInsightFacts;

/**
 * 통계 Fact를 사람이 읽을 형태로 만든다.
 *
 * {@link InsightGenerator}에 Overload를 더하지 않고 계약을 나눴다. 더하면
 * {@code OpenAiInsightGenerator}도 통계용 구현을 갖게 되는데, 통계 Prompt 설계는
 * #166의 범위다.
 */
public interface StatisticsInsightGenerator {

    StatisticsInsightPreview generate(StatisticsInsightFacts facts);
}
