package com.maplemetric.statistics.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * 한 대상의 기간별 통계 사실이다.
 *
 * {@code range.missingDateCount}는 요청 기간에서 수집이 없던 날 수다. 이 값이 크면
 * 표본이 듬성듬성한 것이라 "꾸준히 늘었다" 같은 서술을 만들 근거가 되지 않는다.
 *
 * {@code points}가 2개 미만이면 {@code rangeComparison}이 {@code null}이다. 비교할
 * 대상이 없는데 변화를 말하면 없는 추세를 만들어낸다.
 */
public record StatisticsHistoryFact(
        StatisticsSubject subject,
        StatisticsDataAvailability dataAvailability,
        RangeFact range,
        RangeComparisonFact rangeComparison,
        List<PointFact> points,
        List<String> limitations
) {

    public StatisticsHistoryFact {
        points = points == null ? List.of() : List.copyOf(points);
        limitations = limitations == null
                ? List.of()
                : List.copyOf(limitations);
    }

    public record RangeFact(
            String preset,
            LocalDate requestedFrom,
            LocalDate requestedTo,
            LocalDate firstAsOf,
            LocalDate lastAsOf,
            int pointCount,
            int missingDateCount
    ) {
    }

    /**
     * 기간의 처음과 끝을 비교한 결과다.
     *
     * 요청 기간이 아니라 실제 수집이 있던 첫 날과 마지막 날을 쓴다. 빈 날을 포함해
     * 비교하면 없는 날의 변화까지 말하게 된다.
     */
    public record RangeComparisonFact(
            LocalDate fromAsOf,
            LocalDate toAsOf,
            Long previousCount,
            long currentCount,
            Long countChange,
            BigDecimal countChangeRate,
            BigDecimal previousPercentage,
            BigDecimal currentPercentage,
            BigDecimal percentageChangeRate,
            BigDecimal percentagePointChange
    ) {
    }

    public record PointFact(
            LocalDate asOf,
            long count,
            BigDecimal percentage,
            BigDecimal averageLevel,
            int sampleSize,
            int pageCount,
            int requestedMaxPages,
            boolean truncated,
            Instant collectedAt
    ) {
    }
}
