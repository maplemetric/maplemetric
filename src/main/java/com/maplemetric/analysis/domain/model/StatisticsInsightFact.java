package com.maplemetric.analysis.domain.model;

import com.maplemetric.statistics.api.StatisticsTrend;
import java.math.BigDecimal;
import java.util.List;

/**
 * 통계에서 읽어낸 관찰 하나다.
 *
 * {@code value}는 Statistics가 반올림까지 마친 값 그대로다. Analysis가 다시 계산하면
 * 화면과 설명이 서로 다른 숫자를 말하게 된다.
 *
 * {@code trend}는 Statistics가 판정한 값만 담는다. Statistics가 판정하지 않은 수치는
 * {@code null}이며, 값의 부호를 보고 방향을 지어내지 않는다.
 *
 * {@code value}가 {@code null}일 수 있는 것은 {@code INSUFFICIENT_DATA}뿐이다. 말할
 * 수치가 없다는 사실 자체가 그 Fact의 내용이다.
 */
public record StatisticsInsightFact(
        StatisticsInsightFactType type,
        BigDecimal value,
        StatisticsInsightUnit unit,
        StatisticsTrend trend,
        List<InsightEvidence> evidence
) {

    public StatisticsInsightFact {
        if (type == null) {
            throw new IllegalArgumentException(
                    "통계 인사이트 Fact 종류는 비어 있을 수 없습니다."
            );
        }

        if (type != StatisticsInsightFactType.INSUFFICIENT_DATA
                && value == null) {
            throw new IllegalArgumentException(
                    "통계 인사이트 Fact 수치는 비어 있을 수 없습니다."
            );
        }

        if (value != null && unit == null) {
            throw new IllegalArgumentException(
                    "통계 인사이트 Fact 단위는 비어 있을 수 없습니다."
            );
        }

        evidence = evidence == null ? List.of() : List.copyOf(evidence);
    }
}
