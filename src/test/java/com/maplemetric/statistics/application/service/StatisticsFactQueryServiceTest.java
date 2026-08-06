package com.maplemetric.statistics.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.maplemetric.ranking.api.CanonicalJob;
import com.maplemetric.statistics.api.StatisticsDataAvailability;
import com.maplemetric.statistics.api.StatisticsDetailFact;
import com.maplemetric.statistics.api.StatisticsHistoryFact;
import com.maplemetric.statistics.api.StatisticsSubjectType;
import com.maplemetric.statistics.api.StatisticsTrend;
import com.maplemetric.statistics.application.result.GetJobStatisticsDetailResult;
import com.maplemetric.statistics.application.result.GetJobStatisticsHistoryResult;
import com.maplemetric.statistics.application.result.GetWorldStatisticsDetailResult;
import com.maplemetric.statistics.application.result.GetWorldStatisticsHistoryResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StatisticsFactQueryServiceTest {

    private static final LocalDate AS_OF = LocalDate.of(2026, 7, 25);

    private static final LocalDate PREVIOUS_AS_OF = LocalDate.of(2026, 7, 24);

    private static final Instant COLLECTED_AT =
            Instant.parse("2026-07-25T00:40:00Z");

    @Mock
    private JobStatisticsDetailQueryService jobQueryService;

    @Mock
    private WorldStatisticsDetailQueryService worldQueryService;

    @Mock
    private com.maplemetric.ranking.api.JobCatalogQuery jobCatalogQuery;

    @Mock
    private com.maplemetric.world.api.WorldCatalogQuery worldCatalogQuery;

    private StatisticsFactQueryService service;

    @BeforeEach
    void setUp() {
        service = new StatisticsFactQueryService(
                jobQueryService,
                worldQueryService,
                jobCatalogQuery,
                worldCatalogQuery
        );
    }

    /**
     * 소비자가 직업·월드 Catalog를 각각 알 필요 없이 한 계약으로 순회한다.
     */
    @Test
    void 직업다음월드순서로대상을돌려준다() {
        given(jobCatalogQuery.findAll()).willReturn(List.of(
                new CanonicalJob("hero", "히어로", "모험가", "전사", true, 1)
        ));
        given(worldCatalogQuery.findAll()).willReturn(List.of(
                new com.maplemetric.world.api.CanonicalWorld(
                        "scania",
                        "스카니아",
                        com.maplemetric.world.api.CanonicalWorld.Status.ACTIVE,
                        1
                )
        ));

        assertThat(service.listSubjects())
                .extracting(
                        subject -> subject.type(),
                        subject -> subject.slug()
                )
                .containsExactly(
                        org.assertj.core.api.Assertions.tuple(
                                StatisticsSubjectType.JOB, "hero"
                        ),
                        org.assertj.core.api.Assertions.tuple(
                                StatisticsSubjectType.WORLD, "scania"
                        )
                );
    }

    @Test
    void 직업Detail의수치와단위를그대로옮긴다() {
        given(jobQueryService.getJobStatisticsDetail("hero"))
                .willReturn(new GetJobStatisticsDetailResult(
                        new GetJobStatisticsDetailResult.JobResult(
                                "hero",
                                "히어로",
                                "전사",
                                "모험가",
                                true
                        ),
                        StatisticsDataAvailability.AVAILABLE,
                        new GetJobStatisticsDetailResult.LatestResult(
                                AS_OF,
                                240L,
                                new BigDecimal("12.34"),
                                new BigDecimal("287.6")
                        ),
                        new GetJobStatisticsDetailResult.ComparisonResult(
                                PREVIOUS_AS_OF,
                                200L,
                                40L,
                                new BigDecimal("20.00"),
                                new BigDecimal("10.00"),
                                new BigDecimal("23.40"),
                                new BigDecimal("2.34"),
                                StatisticsTrend.UP
                        ),
                        new GetJobStatisticsDetailResult.SourceMetaResult(
                                "NEXON_OPEN_API",
                                COLLECTED_AT,
                                2000,
                                10,
                                10,
                                true
                        )
                ));

        StatisticsDetailFact fact = service.getJobDetailFact("hero");

        assertThat(fact.subject())
                .extracting(
                        subject -> subject.type(),
                        subject -> subject.slug(),
                        subject -> subject.name()
                )
                .containsExactly(StatisticsSubjectType.JOB, "hero", "히어로");

        assertThat(fact.dataAvailability())
                .isEqualTo(StatisticsDataAvailability.AVAILABLE);

        assertThat(fact.latest())
                .isEqualTo(new StatisticsDetailFact.LatestFact(
                        AS_OF,
                        240L,
                        new BigDecimal("12.34"),
                        new BigDecimal("287.6")
                ));

        // 상대 변화율 %와 Percentage Point %p는 끝까지 분리해서 옮긴다.
        assertThat(fact.comparison())
                .isEqualTo(new StatisticsDetailFact.ComparisonFact(
                        PREVIOUS_AS_OF,
                        200L,
                        40L,
                        new BigDecimal("20.00"),
                        new BigDecimal("10.00"),
                        new BigDecimal("23.40"),
                        new BigDecimal("2.34"),
                        StatisticsTrend.UP
                ));

        assertThat(fact.sourceMeta())
                .isEqualTo(new StatisticsDetailFact.SourceMetaFact(
                        "NEXON_OPEN_API",
                        COLLECTED_AT,
                        2000,
                        10,
                        10,
                        true
                ));
    }

    @Test
    void 월드Detail은대상종류만다르게옮긴다() {
        given(worldQueryService.getWorldStatisticsDetail("scania"))
                .willReturn(new GetWorldStatisticsDetailResult(
                        new GetWorldStatisticsDetailResult.WorldResult(
                                "scania",
                                "스카니아",
                                null,
                                1
                        ),
                        StatisticsDataAvailability.AVAILABLE,
                        new GetWorldStatisticsDetailResult.LatestResult(
                                AS_OF,
                                180L,
                                new BigDecimal("9.00"),
                                new BigDecimal("270.1")
                        ),
                        null,
                        null
                ));

        StatisticsDetailFact fact = service.getWorldDetailFact("scania");

        assertThat(fact.subject().type())
                .isEqualTo(StatisticsSubjectType.WORLD);
        assertThat(fact.subject().slug()).isEqualTo("scania");
        assertThat(fact.latest().count()).isEqualTo(180L);
    }

    /**
     * 수집이 없는 것과 Count 0은 다르다.
     */
    @Test
    void 수집이없으면수치를비워둔다() {
        given(jobQueryService.getJobStatisticsDetail("hero"))
                .willReturn(GetJobStatisticsDetailResult.notCollected(
                        new CanonicalJob(
                                "hero",
                                "히어로",
                                "전사",
                                "모험가",
                                true,
                                1
                        )
                ));

        StatisticsDetailFact fact = service.getJobDetailFact("hero");

        assertThat(fact.dataAvailability())
                .isEqualTo(StatisticsDataAvailability.NOT_COLLECTED);
        assertThat(fact.latest()).isNull();
        assertThat(fact.comparison()).isNull();
        assertThat(fact.sourceMeta()).isNull();
        assertThat(fact.subject().slug()).isEqualTo("hero");
    }

    @Test
    void 직업History의구간과Point를순서대로옮긴다() {
        given(jobQueryService.getJobStatisticsHistory("hero", "7D", null, null))
                .willReturn(new GetJobStatisticsHistoryResult(
                        new GetJobStatisticsHistoryResult.JobResult(
                                "hero",
                                "히어로"
                        ),
                        StatisticsDataAvailability.AVAILABLE,
                        new GetJobStatisticsHistoryResult.RangeResult(
                                "7D",
                                PREVIOUS_AS_OF,
                                AS_OF,
                                PREVIOUS_AS_OF,
                                AS_OF,
                                2,
                                0
                        ),
                        new GetJobStatisticsHistoryResult
                                .RangeComparisonResult(
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
                        ),
                        List.of(
                                point(PREVIOUS_AS_OF, 200L, "10.00"),
                                point(AS_OF, 240L, "12.34")
                        ),
                        List.of("표본이 절단되었습니다.")
                ));

        StatisticsHistoryFact fact =
                service.getJobHistoryFact("hero", "7D", null, null);

        assertThat(fact.range())
                .isEqualTo(new StatisticsHistoryFact.RangeFact(
                        "7D",
                        PREVIOUS_AS_OF,
                        AS_OF,
                        PREVIOUS_AS_OF,
                        AS_OF,
                        2,
                        0
                ));

        assertThat(fact.points())
                .extracting(
                        pointFact -> pointFact.asOf(),
                        pointFact -> pointFact.count(),
                        pointFact -> pointFact.percentage()
                )
                .containsExactly(
                        org.assertj.core.api.Assertions.tuple(
                                PREVIOUS_AS_OF, 200L, new BigDecimal("10.00")
                        ),
                        org.assertj.core.api.Assertions.tuple(
                                AS_OF, 240L, new BigDecimal("12.34")
                        )
                );

        assertThat(fact.rangeComparison().percentagePointChange())
                .isEqualByComparingTo("2.34");
        assertThat(fact.limitations())
                .containsExactly("표본이 절단되었습니다.");
    }

    /**
     * Point가 부족하면 비교할 대상이 없다. 없는 추세를 만들지 않는다.
     */
    @Test
    void 구간비교가없으면비워서옮긴다() {
        given(worldQueryService.getWorldStatisticsHistory(
                "scania",
                "30D",
                null,
                null
        )).willReturn(new GetWorldStatisticsHistoryResult(
                new GetWorldStatisticsHistoryResult.WorldResult(
                        "scania",
                        "스카니아"
                ),
                StatisticsDataAvailability.AVAILABLE,
                new GetWorldStatisticsHistoryResult.RangeResult(
                        "30D",
                        PREVIOUS_AS_OF,
                        AS_OF,
                        AS_OF,
                        AS_OF,
                        1,
                        1
                ),
                null,
                List.of(),
                List.of()
        ));

        StatisticsHistoryFact fact =
                service.getWorldHistoryFact("scania", "30D", null, null);

        assertThat(fact.rangeComparison()).isNull();
        assertThat(fact.points()).isEmpty();

        // 요청 기간에서 비어 있던 날 수를 잃지 않는다.
        assertThat(fact.range().missingDateCount()).isEqualTo(1);
    }

    private GetJobStatisticsHistoryResult.PointResult point(
            LocalDate asOf,
            long count,
            String percentage
    ) {
        return new GetJobStatisticsHistoryResult.PointResult(
                asOf,
                count,
                new BigDecimal(percentage),
                new BigDecimal("280.0"),
                2000,
                10,
                10,
                false,
                COLLECTED_AT
        );
    }
}
