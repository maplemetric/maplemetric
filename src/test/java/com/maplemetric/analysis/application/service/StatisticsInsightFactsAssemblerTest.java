package com.maplemetric.analysis.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.maplemetric.analysis.domain.model.StatisticsInsightFactType;
import com.maplemetric.analysis.domain.model.StatisticsInsightFacts;
import com.maplemetric.analysis.domain.model.StatisticsInsightUnit;
import com.maplemetric.statistics.api.StatisticsDataAvailability;
import com.maplemetric.statistics.api.StatisticsDetailFact;
import com.maplemetric.statistics.api.StatisticsHistoryFact;
import com.maplemetric.statistics.api.StatisticsSubject;
import com.maplemetric.statistics.api.StatisticsSubjectType;
import com.maplemetric.statistics.api.StatisticsTrend;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class StatisticsInsightFactsAssemblerTest {

    private static final LocalDate AS_OF = LocalDate.of(2026, 7, 25);

    private static final LocalDate PREVIOUS_AS_OF = LocalDate.of(2026, 7, 24);

    private static final Instant COLLECTED_AT =
            Instant.parse("2026-07-25T00:40:00Z");

    private final StatisticsInsightFactsAssembler assembler =
            new StatisticsInsightFactsAssembler();

    @Test
    void 최신통계의수치와단위를그대로옮긴다() {
        StatisticsInsightFacts facts =
                assembler.assemble(detail(comparison(StatisticsTrend.UP)));

        assertThat(facts.subject())
                .isEqualTo(new StatisticsInsightFacts.Subject(
                        StatisticsSubjectType.JOB,
                        "hero",
                        "히어로"
                ));

        assertThat(facts.facts())
                .extracting(
                        fact -> fact.type(),
                        fact -> fact.value(),
                        fact -> fact.unit()
                )
                .containsExactly(
                        org.assertj.core.api.Assertions.tuple(
                                StatisticsInsightFactType.SHARE_CHANGE,
                                new BigDecimal("2.34"),
                                StatisticsInsightUnit.PERCENTAGE_POINT
                        ),
                        org.assertj.core.api.Assertions.tuple(
                                StatisticsInsightFactType.COUNT_CHANGE,
                                BigDecimal.valueOf(40L),
                                StatisticsInsightUnit.COUNT
                        ),
                        org.assertj.core.api.Assertions.tuple(
                                StatisticsInsightFactType.SHARE,
                                new BigDecimal("12.34"),
                                StatisticsInsightUnit.PERCENT
                        ),
                        org.assertj.core.api.Assertions.tuple(
                                StatisticsInsightFactType.COUNT,
                                BigDecimal.valueOf(240L),
                                StatisticsInsightUnit.COUNT
                        ),
                        org.assertj.core.api.Assertions.tuple(
                                StatisticsInsightFactType.AVERAGE_LEVEL,
                                new BigDecimal("287.6"),
                                StatisticsInsightUnit.LEVEL
                        )
                );
    }

    /**
     * 방향은 Statistics가 판정한 값만 쓴다. 부호를 보고 지어내지 않는다.
     */
    @Test
    void Statistics가판정한Trend만담는다() {
        StatisticsInsightFacts facts =
                assembler.assemble(detail(comparison(StatisticsTrend.DOWN)));

        assertThat(facts.facts())
                .filteredOn(fact ->
                        fact.type() == StatisticsInsightFactType.SHARE_CHANGE)
                .singleElement()
                .extracting(fact -> fact.trend())
                .isEqualTo(StatisticsTrend.DOWN);

        assertThat(facts.facts())
                .filteredOn(fact ->
                        fact.type() == StatisticsInsightFactType.COUNT_CHANGE)
                .singleElement()
                .extracting(fact -> fact.trend())
                .isNull();
    }

    @Test
    void 표본이잘리면한계를남긴다() {
        StatisticsInsightFacts facts =
                assembler.assemble(detail(comparison(StatisticsTrend.UP)));

        assertThat(facts.limitations())
                .containsExactly("표본이 상위 2000명으로 잘려 전체 사용자가 아닙니다.");
        assertThat(facts.source().truncated()).isTrue();
    }

    @Test
    void 수집이없으면수치대신데이터부족만남긴다() {
        StatisticsInsightFacts facts = assembler.assemble(
                new StatisticsDetailFact(
                        subject(),
                        StatisticsDataAvailability.NOT_COLLECTED,
                        null,
                        null,
                        null
                )
        );

        assertThat(facts.facts())
                .singleElement()
                .extracting(fact -> fact.type(), fact -> fact.value())
                .containsExactly(
                        StatisticsInsightFactType.INSUFFICIENT_DATA,
                        null
                );

        assertThat(facts.limitations())
                .containsExactly("변화를 말할 만큼 수집된 기준일이 없습니다.");
    }

    @Test
    void 비교할지점이없으면변화를만들지않는다() {
        StatisticsInsightFacts facts = assembler.assemble(
                history(1, 0, null)
        );

        assertThat(facts.facts())
                .extracting(fact -> fact.type())
                .containsExactly(StatisticsInsightFactType.INSUFFICIENT_DATA);
    }

    static Stream<Arguments> 관측점정책표() {
        return Stream.of(
                // 관측점 수, 빈 날, 변화를 말할 수 있는가
                Arguments.of(2, 0, false),
                Arguments.of(3, 0, true),

                // 빈 날이 아무리 많아도 관측점으로만 판단한다.
                // 주 1회 표본 추출이라 90일 창에 13점(빈 날 77)이 정상이다.
                Arguments.of(13, 77, true),
                Arguments.of(22, 68, true),
                Arguments.of(2, 88, false)
        );
    }

    /**
     * 관측점이 추세를 말할 만큼 모여야 변화를 만든다.
     *
     * 달력일 대비 수집 비율로 판단하지 않는다. 수집이 주 1회 표본 추출이므로
     * 비율로 막으면 정상 표본이 전부 걸린다.
     */
    @ParameterizedTest
    @MethodSource("관측점정책표")
    void 관측점이부족하면변화를만들지않는다(
            int pointCount,
            int missingDateCount,
            boolean expectedChangeFact
    ) {
        StatisticsInsightFacts facts = assembler.assemble(
                history(pointCount, missingDateCount, rangeComparison())
        );

        assertThat(
                facts.facts()
                        .stream()
                        .anyMatch(fact -> fact.type()
                                == StatisticsInsightFactType.SHARE_CHANGE)
        ).isEqualTo(expectedChangeFact);
    }

    @Test
    void 빈날이있으면몇일인지한계로남긴다() {
        StatisticsInsightFacts facts = assembler.assemble(
                history(6, 4, rangeComparison())
        );

        assertThat(facts.limitations())
                .contains("요청 기간 중 4일은 수집이 없습니다.");
        assertThat(facts.period().missingDateCount()).isEqualTo(4);
    }

    /**
     * 같은 입력이면 같은 목록과 같은 순서가 나와야 한다.
     */
    @Test
    void 같은입력은같은결과를만든다() {
        StatisticsDetailFact input = detail(comparison(StatisticsTrend.UP));

        assertThat(assembler.assemble(input))
                .isEqualTo(assembler.assemble(input));
    }

    private StatisticsSubject subject() {
        return new StatisticsSubject(
                StatisticsSubjectType.JOB,
                "hero",
                "히어로"
        );
    }

    private StatisticsDetailFact.ComparisonFact comparison(
            StatisticsTrend trend
    ) {
        return new StatisticsDetailFact.ComparisonFact(
                PREVIOUS_AS_OF,
                200L,
                40L,
                new BigDecimal("20.00"),
                new BigDecimal("10.00"),
                new BigDecimal("23.40"),
                new BigDecimal("2.34"),
                trend
        );
    }

    private StatisticsDetailFact detail(
            StatisticsDetailFact.ComparisonFact comparison
    ) {
        return new StatisticsDetailFact(
                subject(),
                StatisticsDataAvailability.AVAILABLE,
                new StatisticsDetailFact.LatestFact(
                        AS_OF,
                        240L,
                        new BigDecimal("12.34"),
                        new BigDecimal("287.6")
                ),
                comparison,
                new StatisticsDetailFact.SourceMetaFact(
                        "NEXON_OPEN_API",
                        COLLECTED_AT,
                        2000,
                        10,
                        10,
                        true
                )
        );
    }

    private StatisticsHistoryFact.RangeComparisonFact rangeComparison() {
        return new StatisticsHistoryFact.RangeComparisonFact(
                PREVIOUS_AS_OF,
                AS_OF,
                200L,
                240L,
                40L,
                new BigDecimal("20.00"),
                new BigDecimal("10.00"),
                new BigDecimal("12.34"),
                new BigDecimal("23.40"),
                new BigDecimal("2.34")
        );
    }

    private StatisticsHistoryFact history(
            int pointCount,
            int missingDateCount,
            StatisticsHistoryFact.RangeComparisonFact rangeComparison
    ) {
        return new StatisticsHistoryFact(
                subject(),
                StatisticsDataAvailability.AVAILABLE,
                new StatisticsHistoryFact.RangeFact(
                        "30D",
                        PREVIOUS_AS_OF,
                        AS_OF,
                        PREVIOUS_AS_OF,
                        AS_OF,
                        pointCount,
                        missingDateCount
                ),
                rangeComparison,
                List.of(new StatisticsHistoryFact.PointFact(
                        AS_OF,
                        240L,
                        new BigDecimal("12.34"),
                        new BigDecimal("287.6"),
                        2000,
                        10,
                        10,
                        false,
                        COLLECTED_AT
                )),
                List.of()
        );
    }
}
