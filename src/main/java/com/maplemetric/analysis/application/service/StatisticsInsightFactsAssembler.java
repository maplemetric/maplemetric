package com.maplemetric.analysis.application.service;

import com.maplemetric.analysis.domain.model.InsightEvidence;
import com.maplemetric.analysis.domain.model.StatisticsInsightFact;
import com.maplemetric.analysis.domain.model.StatisticsInsightFactType;
import com.maplemetric.analysis.domain.model.StatisticsInsightFacts;
import com.maplemetric.analysis.domain.model.StatisticsInsightUnit;
import com.maplemetric.statistics.api.StatisticsDataAvailability;
import com.maplemetric.statistics.api.StatisticsDetailFact;
import com.maplemetric.statistics.api.StatisticsHistoryFact;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Statistics가 검증한 수치를 설명 재료로 옮긴다.
 *
 * 계산하지 않는다. Statistics가 반올림과 Trend 판정까지 마친 값을 그대로 옮기며,
 * 여기서 다시 계산하면 화면과 설명이 서로 다른 숫자를 말하게 된다.
 *
 * 말할 근거가 없으면 만들지 않는다. 수집이 없거나, 비교할 지점이 없거나, 관측점이
 * 추세를 말할 만큼 모이지 않았으면 변화 Fact 대신 데이터 부족 Fact만 남긴다.
 */
@Service
public class StatisticsInsightFactsAssembler {

    /**
     * 구간 변화를 말할 수 있는 최소 관측점 수다.
     *
     * 두 점은 "차이"일 뿐 "추세"가 아니다. 중간 관측이 하나라도 있어야 방향이
     * 우연한 양 끝 값인지 아닌지를 볼 수 있다.
     *
     * 달력일 대비 수집 비율로 판단하지 않는다. 수집은 주 1회 표본 추출이라(T10)
     * 90일 창에 13점(14%)이 정상이며, 비율로 막으면 정상 표본이 전부 걸린다.
     * 실제로 그 상태가 나왔다. 수집 범위는 {@code Period.missingDateCount}와
     * limitations로 이미 응답에 나가므로 소비자가 판단할 수 있다.
     */
    static final int MINIMUM_POINT_COUNT = 3;

    public StatisticsInsightFacts assemble(StatisticsDetailFact detail) {
        StatisticsInsightFacts.Subject subject = toSubject(detail);

        if (detail.dataAvailability()
                != StatisticsDataAvailability.AVAILABLE) {
            return notCollected(subject, null);
        }

        List<StatisticsInsightFact> facts = new ArrayList<>();
        StatisticsDetailFact.LatestFact latest = detail.latest();

        addShareAndCount(facts, latest.percentage(), latest.count());

        if (latest.averageLevel() != null) {
            facts.add(new StatisticsInsightFact(
                    StatisticsInsightFactType.AVERAGE_LEVEL,
                    latest.averageLevel(),
                    StatisticsInsightUnit.LEVEL,
                    null,
                    List.of(evidence("평균 레벨", latest.averageLevel()))
            ));
        }

        addDetailChanges(facts, detail.comparison());

        return new StatisticsInsightFacts(
                subject,
                new StatisticsInsightFacts.Period(
                        null,
                        detail.comparison() == null
                                ? latest.asOf()
                                : detail.comparison().previousAsOf(),
                        latest.asOf(),
                        detail.comparison() == null ? 1 : 2,
                        0
                ),
                facts,
                toSource(detail.sourceMeta()),
                toLimitations(detail.sourceMeta(), 0)
        );
    }

    public StatisticsInsightFacts assemble(StatisticsHistoryFact history) {
        StatisticsInsightFacts.Subject subject = toSubject(history);
        StatisticsHistoryFact.RangeFact range = history.range();

        StatisticsInsightFacts.Period period = new StatisticsInsightFacts.Period(
                range == null ? null : range.preset(),
                range == null ? null : range.firstAsOf(),
                range == null ? null : range.lastAsOf(),
                range == null ? 0 : range.pointCount(),
                range == null ? 0 : range.missingDateCount()
        );

        if (history.dataAvailability() != StatisticsDataAvailability.AVAILABLE
                || history.rangeComparison() == null
                || !hasEnoughPoints(range)) {
            return notCollected(subject, period);
        }

        List<StatisticsInsightFact> facts = new ArrayList<>();
        StatisticsHistoryFact.RangeComparisonFact comparison =
                history.rangeComparison();

        addShareAndCount(
                facts,
                comparison.currentPercentage(),
                comparison.currentCount()
        );

        addRangeChanges(facts, comparison);

        return new StatisticsInsightFacts(
                subject,
                period,
                facts,
                toSource(history.points()),
                toLimitations(
                        history.points(),
                        period.missingDateCount(),
                        history.limitations()
                )
        );
    }

    /**
     * 추세를 말할 만큼 관측점이 있는지 본다.
     */
    private boolean hasEnoughPoints(
            StatisticsHistoryFact.RangeFact range
    ) {
        return range != null && range.pointCount() >= MINIMUM_POINT_COUNT;
    }

    private void addShareAndCount(
            List<StatisticsInsightFact> facts,
            BigDecimal percentage,
            long count
    ) {
        if (percentage != null) {
            facts.add(new StatisticsInsightFact(
                    StatisticsInsightFactType.SHARE,
                    percentage,
                    StatisticsInsightUnit.PERCENT,
                    null,
                    List.of(evidence("비중", percentage))
            ));
        }

        facts.add(new StatisticsInsightFact(
                StatisticsInsightFactType.COUNT,
                BigDecimal.valueOf(count),
                StatisticsInsightUnit.COUNT,
                null,
                List.of(evidence("표본 수", BigDecimal.valueOf(count)))
        ));
    }

    private void addDetailChanges(
            List<StatisticsInsightFact> facts,
            StatisticsDetailFact.ComparisonFact comparison
    ) {
        if (comparison == null) {
            return;
        }

        if (comparison.percentagePointChange() != null) {
            facts.add(new StatisticsInsightFact(
                    StatisticsInsightFactType.SHARE_CHANGE,
                    comparison.percentagePointChange(),
                    StatisticsInsightUnit.PERCENTAGE_POINT,
                    comparison.trend(),
                    List.of(
                            evidence(
                                    "이전 비중",
                                    comparison.previousPercentage()
                            ),
                            evidence(
                                    "비중 변화",
                                    comparison.percentagePointChange()
                            )
                    )
            ));
        }

        if (comparison.countChange() != null) {
            facts.add(new StatisticsInsightFact(
                    StatisticsInsightFactType.COUNT_CHANGE,
                    BigDecimal.valueOf(comparison.countChange()),
                    StatisticsInsightUnit.COUNT,
                    null,
                    List.of(evidence(
                            "표본 수 변화",
                            BigDecimal.valueOf(comparison.countChange())
                    ))
            ));
        }
    }

    private void addRangeChanges(
            List<StatisticsInsightFact> facts,
            StatisticsHistoryFact.RangeComparisonFact comparison
    ) {
        if (comparison.percentagePointChange() != null) {
            facts.add(new StatisticsInsightFact(
                    StatisticsInsightFactType.SHARE_CHANGE,
                    comparison.percentagePointChange(),
                    StatisticsInsightUnit.PERCENTAGE_POINT,
                    null,
                    List.of(
                            evidence(
                                    "구간 시작 비중",
                                    comparison.previousPercentage()
                            ),
                            evidence(
                                    "비중 변화",
                                    comparison.percentagePointChange()
                            )
                    )
            ));
        }

        if (comparison.countChange() != null) {
            facts.add(new StatisticsInsightFact(
                    StatisticsInsightFactType.COUNT_CHANGE,
                    BigDecimal.valueOf(comparison.countChange()),
                    StatisticsInsightUnit.COUNT,
                    null,
                    List.of(evidence(
                            "표본 수 변화",
                            BigDecimal.valueOf(comparison.countChange())
                    ))
            ));
        }
    }

    private StatisticsInsightFacts notCollected(
            StatisticsInsightFacts.Subject subject,
            StatisticsInsightFacts.Period period
    ) {
        return new StatisticsInsightFacts(
                subject,
                period,
                List.of(new StatisticsInsightFact(
                        StatisticsInsightFactType.INSUFFICIENT_DATA,
                        null,
                        null,
                        com.maplemetric.statistics.api.StatisticsTrend
                                .INSUFFICIENT_DATA,
                        List.of()
                )),
                null,
                List.of("변화를 말할 만큼 수집된 기준일이 없습니다.")
        );
    }

    private StatisticsInsightFacts.Subject toSubject(
            StatisticsDetailFact detail
    ) {
        return new StatisticsInsightFacts.Subject(
                detail.subject().type(),
                detail.subject().slug(),
                detail.subject().name()
        );
    }

    private StatisticsInsightFacts.Subject toSubject(
            StatisticsHistoryFact history
    ) {
        return new StatisticsInsightFacts.Subject(
                history.subject().type(),
                history.subject().slug(),
                history.subject().name()
        );
    }

    private StatisticsInsightFacts.Source toSource(
            StatisticsDetailFact.SourceMetaFact sourceMeta
    ) {
        if (sourceMeta == null) {
            return null;
        }

        return new StatisticsInsightFacts.Source(
                sourceMeta.source(),
                sourceMeta.collectedAt(),
                sourceMeta.sampleSize(),
                sourceMeta.truncated()
        );
    }

    /**
     * 구간의 출처는 마지막 Point를 쓴다.
     *
     * 가장 최근 수집이 어떤 조건이었는지가 그 구간을 대표한다.
     */
    private StatisticsInsightFacts.Source toSource(
            List<StatisticsHistoryFact.PointFact> points
    ) {
        if (points.isEmpty()) {
            return null;
        }

        StatisticsHistoryFact.PointFact last = points.get(points.size() - 1);

        return new StatisticsInsightFacts.Source(
                null,
                last.collectedAt(),
                last.sampleSize(),
                points.stream().anyMatch(point -> point.truncated())
        );
    }

    private List<String> toLimitations(
            StatisticsDetailFact.SourceMetaFact sourceMeta,
            int missingDateCount
    ) {
        List<String> limitations = new ArrayList<>();

        if (sourceMeta != null && sourceMeta.truncated()) {
            limitations.add(truncatedLimitation(sourceMeta.sampleSize()));
        }

        addMissingLimitation(limitations, missingDateCount);

        return limitations;
    }

    private List<String> toLimitations(
            List<StatisticsHistoryFact.PointFact> points,
            int missingDateCount,
            List<String> inherited
    ) {
        List<String> limitations = new ArrayList<>(inherited);

        points.stream()
                .filter(point -> point.truncated())
                .findFirst()
                .ifPresent(point -> limitations.add(
                        truncatedLimitation(point.sampleSize())
                ));

        addMissingLimitation(limitations, missingDateCount);

        return limitations;
    }

    private void addMissingLimitation(
            List<String> limitations,
            int missingDateCount
    ) {
        if (missingDateCount > 0) {
            limitations.add(
                    "요청 기간 중 " + missingDateCount
                            + "일은 수집이 없습니다."
            );
        }
    }

    private String truncatedLimitation(Integer sampleSize) {
        return "표본이 상위 " + sampleSize
                + "명으로 잘려 전체 사용자가 아닙니다.";
    }

    private InsightEvidence evidence(
            String label,
            BigDecimal value
    ) {
        return new InsightEvidence(label, value.toPlainString());
    }
}
