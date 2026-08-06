package com.maplemetric.statistics.presentation.response;

import com.maplemetric.statistics.application.result.GetWorldStatisticsHistoryResult;
import com.maplemetric.statistics.api.StatisticsDataAvailability;
import com.maplemetric.statistics.api.StatisticsTrend;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record GetWorldStatisticsHistoryResponse(
        World world,
        StatisticsDataAvailability dataAvailability,
        Range range,
        RangeComparison rangeComparison,
        List<Point> points,
        List<String> limitations
) {

    public static GetWorldStatisticsHistoryResponse from(
            GetWorldStatisticsHistoryResult result
    ) {
        List<Point> points = result.points().stream()
                .map(point -> new Point(
                        point.asOf(),
                        point.count(),
                        point.percentage(),
                        point.averageLevel(),
                        point.sampleSize(),
                        point.pageCount(),
                        point.requestedMaxPages(),
                        point.truncated(),
                        point.collectedAt()
                ))
                .toList();

        return new GetWorldStatisticsHistoryResponse(
                new World(
                        result.world().worldSlug(),
                        result.world().worldName()
                ),
                result.dataAvailability(),
                new Range(
                        result.range().preset(),
                        result.range().requestedFrom(),
                        result.range().requestedTo(),
                        result.range().firstAsOf(),
                        result.range().lastAsOf(),
                        result.range().pointCount(),
                        result.range().missingDateCount()
                ),
                result.rangeComparison() == null
                        ? null
                        : new RangeComparison(
                                result.rangeComparison().fromAsOf(),
                                result.rangeComparison().toAsOf(),
                                result.rangeComparison().previousCount(),
                                result.rangeComparison().currentCount(),
                                result.rangeComparison().countChange(),
                                result.rangeComparison().countChangeRate(),
                                result.rangeComparison()
                                        .previousPercentage(),
                                result.rangeComparison()
                                        .currentPercentage(),
                                result.rangeComparison()
                                        .percentageChangeRate(),
                                result.rangeComparison()
                                        .percentagePointChange(),
                                result.rangeComparison().trend()
                        ),
                points,
                result.limitations()
        );
    }

    public record World(
            String worldSlug,
            String worldName
    ) {
    }

    public record Range(
            String preset,
            LocalDate requestedFrom,
            LocalDate requestedTo,
            LocalDate firstAsOf,
            LocalDate lastAsOf,
            int pointCount,
            int missingDateCount
    ) {
    }

    public record RangeComparison(
            LocalDate fromAsOf,
            LocalDate toAsOf,
            Long previousCount,
            long currentCount,
            Long countChange,
            BigDecimal countChangeRate,
            BigDecimal previousPercentage,
            BigDecimal currentPercentage,
            BigDecimal percentageChangeRate,
            BigDecimal percentagePointChange,
            StatisticsTrend trend
    ) {
    }

    public record Point(
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
