package com.maplemetric.statistics.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * 한 대상의 최신 통계 사실이다.
 *
 * 수치·Trend·반올림은 Statistics가 계산한 결과 그대로다. 소비자가 다시 계산하면
 * 화면과 설명이 서로 다른 숫자를 말하게 된다.
 *
 * {@code dataAvailability}가 {@code NOT_COLLECTED}면 {@code latest}·{@code comparison}
 * ·{@code sourceMeta}가 모두 {@code null}이다. 수집이 없는 것과 실제 Count 0은 다르며,
 * 이 구분을 잃으면 "0명"이라고 단정하게 된다.
 */
public record StatisticsDetailFact(
        StatisticsSubject subject,
        StatisticsDataAvailability dataAvailability,
        LatestFact latest,
        ComparisonFact comparison,
        SourceMetaFact sourceMeta
) {

    public record LatestFact(
            LocalDate asOf,
            long count,
            BigDecimal percentage,
            BigDecimal averageLevel
    ) {
    }

    /**
     * 직전 기준일 대비 변화다.
     *
     * 비율 변화는 두 단위를 나눠 갖는다. {@code percentageChangeRate}는 상대 변화율
     * {@code %}이고 {@code percentagePointChange}는 절대 차이 {@code %p}다. 하나로
     * 합치면 "10%에서 12%로 올랐다"를 20% 증가로도, 2%p 증가로도 읽을 수 있어
     * 설명이 어긋난다.
     */
    public record ComparisonFact(
            LocalDate previousAsOf,
            Long previousCount,
            Long countChange,
            BigDecimal countChangeRate,
            BigDecimal previousPercentage,
            BigDecimal percentageChangeRate,
            BigDecimal percentagePointChange,
            StatisticsTrend trend
    ) {
    }

    /**
     * 수치의 출처다.
     *
     * {@code truncated}가 참이면 표본이 잘린 것이라 전체 모집단이 아니다. 이 값 없이
     * 수치를 인용하면 표본을 전체로 표현하게 된다.
     */
    public record SourceMetaFact(
            String source,
            Instant collectedAt,
            int sampleSize,
            int pageCount,
            int requestedMaxPages,
            boolean truncated
    ) {
    }
}
