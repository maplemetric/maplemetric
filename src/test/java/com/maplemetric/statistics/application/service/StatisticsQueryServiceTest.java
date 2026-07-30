package com.maplemetric.statistics.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.maplemetric.ranking.api.CanonicalJob;
import com.maplemetric.ranking.api.JobCatalogQuery;
import com.maplemetric.ranking.api.OverallRankingComparisonQuery;
import com.maplemetric.ranking.api.OverallRankingComparisonQueryException;
import com.maplemetric.ranking.api.OverallRankingComparisonQueryFailure;
import com.maplemetric.ranking.api.OverallRankingStatisticsComparisonSnapshot;
import com.maplemetric.ranking.api.OverallRankingStatisticsSnapshot;
import com.maplemetric.ranking.api.OverallRankingStatisticsSnapshot.JobCount;
import com.maplemetric.ranking.api.OverallRankingWorldStatisticsQuery;
import com.maplemetric.ranking.api.OverallRankingWorldStatisticsQueryException;
import com.maplemetric.ranking.api.OverallRankingWorldStatisticsQueryFailure;
import com.maplemetric.ranking.api.OverallRankingWorldStatisticsSnapshot;
import com.maplemetric.ranking.api.OverallRankingWorldStatisticsSnapshot.WorldCount;
import com.maplemetric.statistics.application.exception.JobStatisticsException;
import com.maplemetric.statistics.application.exception.JobStatisticsFailure;
import com.maplemetric.statistics.application.result.GetJobStatisticsResult;
import com.maplemetric.statistics.application.result.GetWorldStatisticsResult;
import com.maplemetric.world.api.CanonicalWorld;
import com.maplemetric.world.api.WorldCatalogQuery;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StatisticsQueryServiceTest {

    private static final LocalDate SNAPSHOT_DATE =
            LocalDate.of(2026, 7, 24);

    private static final LocalDate PREVIOUS_SNAPSHOT_DATE =
            LocalDate.of(2026, 7, 21);

    private static final Instant COLLECTED_AT =
            Instant.parse("2026-07-24T01:00:00Z");

    private static final Instant PREVIOUS_COLLECTED_AT =
            Instant.parse("2026-07-21T01:00:00Z");

    private static final CanonicalJob HERO = new CanonicalJob(
            "hero",
            "히어로",
            "모험가",
            "전사",
            true,
            1
    );

    private static final CanonicalJob PHANTOM = new CanonicalJob(
            "phantom",
            "팬텀",
            "기사단",
            "도적",
            true,
            2
    );

    private static final CanonicalJob ADELE = new CanonicalJob(
            "adele",
            "아델",
            "아니마",
            "전사",
            true,
            3
    );

    private static final CanonicalWorld LUNA = new CanonicalWorld(
            "luna",
            "루나",
            CanonicalWorld.Status.ACTIVE,
            1
    );

    private static final CanonicalWorld BERA = new CanonicalWorld(
            "bera",
            "베라",
            CanonicalWorld.Status.ACTIVE,
            2
    );

    @Mock
    private OverallRankingComparisonQuery overallRankingComparisonQuery;

    @Mock
    private OverallRankingWorldStatisticsQuery overallRankingWorldStatisticsQuery;

    @Mock
    private JobCatalogQuery jobCatalogQuery;

    @Mock
    private WorldCatalogQuery worldCatalogQuery;

    @Captor
    private ArgumentCaptor<Collection<String>> aliasNamesCaptor;

    @Test
    void 직업별통계를Slug와백분율평균레벨로변환한다() {
        StatisticsQueryService service = createService();

        givenJobComparison(
                jobSnapshot(
                        SNAPSHOT_DATE,
                        COLLECTED_AT,
                        3,
                        List.of(
                                new JobCount(
                                        "히어로",
                                        1L,
                                        new BigDecimal("200.5")
                                ),
                                new JobCount(
                                        "팬텀",
                                        2L,
                                        new BigDecimal("204.666")
                                )
                        )
                ),
                null,
                null
        );
        givenJobCatalog(
                List.of(HERO, PHANTOM),
                Map.of("히어로", HERO, "팬텀", PHANTOM)
        );

        GetJobStatisticsResult result = service.getJobStatistics();

        assertThat(result.sampleSize()).isEqualTo(3);
        assertThat(result.asOf()).isEqualTo(SNAPSHOT_DATE);
        assertThat(result.jobs())
                .extracting(
                        job -> job.jobSlug(),
                        job -> job.jobName(),
                        job -> job.percentage(),
                        job -> job.averageLevel()
                )
                .containsExactly(
                        tuple(
                                "hero",
                                "히어로",
                                new BigDecimal("33.33"),
                                new BigDecimal("200.5")
                        ),
                        tuple(
                                "phantom",
                                "팬텀",
                                new BigDecimal("66.67"),
                                new BigDecimal("204.7")
                        )
                );
    }

    @Test
    void Canonical직업전체를표시순서대로반환한다() {
        StatisticsQueryService service = createService();

        givenJobComparison(
                jobSnapshot(
                        SNAPSHOT_DATE,
                        COLLECTED_AT,
                        4,
                        List.of(
                                new JobCount("팬텀", 4L, new BigDecimal("200"))
                        )
                ),
                null,
                null
        );
        givenJobCatalog(
                List.of(HERO, PHANTOM, ADELE),
                Map.of("팬텀", PHANTOM)
        );

        GetJobStatisticsResult result = service.getJobStatistics();

        assertThat(result.jobs())
                .extracting(job -> job.jobSlug())
                .containsExactly("hero", "phantom", "adele");
    }

    @Test
    void 표본에없는Canonical직업은Count0과Percentage0과AverageLevel없음으로반환한다() {
        StatisticsQueryService service = createService();

        givenJobComparison(
                jobSnapshot(
                        SNAPSHOT_DATE,
                        COLLECTED_AT,
                        4,
                        List.of(
                                new JobCount("히어로", 4L, new BigDecimal("200"))
                        )
                ),
                null,
                null
        );
        givenJobCatalog(
                List.of(HERO, PHANTOM),
                Map.of("히어로", HERO)
        );

        GetJobStatisticsResult result = service.getJobStatistics();

        assertThat(result.jobs())
                .extracting(
                        job -> job.jobSlug(),
                        job -> job.count(),
                        job -> job.percentage(),
                        job -> job.averageLevel()
                )
                .containsExactly(
                        tuple(
                                "hero",
                                4L,
                                new BigDecimal("100.00"),
                                new BigDecimal("200.0")
                        ),
                        tuple(
                                "phantom",
                                0L,
                                new BigDecimal("0.00"),
                                null
                        )
                );
    }

    @Test
    void 같은Canonical직업에매칭된Alias의Count를합산한다() {
        StatisticsQueryService service = createService();

        givenJobComparison(
                jobSnapshot(
                        SNAPSHOT_DATE,
                        COLLECTED_AT,
                        10,
                        List.of(
                                new JobCount("히어로", 3L, new BigDecimal("200")),
                                new JobCount("Hero", 2L, new BigDecimal("200"))
                        )
                ),
                null,
                null
        );
        givenJobCatalog(
                List.of(HERO),
                Map.of("히어로", HERO, "Hero", HERO)
        );

        GetJobStatisticsResult result = service.getJobStatistics();

        assertThat(result.jobs()).hasSize(1);
        assertThat(result.jobs().get(0).count()).isEqualTo(5L);
        assertThat(result.jobs().get(0).percentage())
                .isEqualTo(new BigDecimal("50.00"));
    }

    @Test
    void 여러Alias의AverageLevel은Count가중평균으로계산한다() {
        StatisticsQueryService service = createService();

        givenJobComparison(
                jobSnapshot(
                        SNAPSHOT_DATE,
                        COLLECTED_AT,
                        1001,
                        List.of(
                                new JobCount("히어로", 1000L, new BigDecimal("200")),
                                new JobCount("Hero", 1L, new BigDecimal("10"))
                        )
                ),
                null,
                null
        );
        givenJobCatalog(
                List.of(HERO),
                Map.of("히어로", HERO, "Hero", HERO)
        );

        GetJobStatisticsResult result = service.getJobStatistics();

        assertThat(result.jobs().get(0).count()).isEqualTo(1001L);
        assertThat(result.jobs().get(0).averageLevel())
                .isEqualTo(new BigDecimal("199.8"))
                .isNotEqualTo(new BigDecimal("105.0"));
    }

    @Test
    void Alias미매칭Row는어떤Canonical직업에도포함하지않는다() {
        StatisticsQueryService service = createService();

        givenJobComparison(
                jobSnapshot(
                        SNAPSHOT_DATE,
                        COLLECTED_AT,
                        10,
                        List.of(
                                new JobCount("히어로", 5L, new BigDecimal("200")),
                                new JobCount("등록되지않은직업", 5L, new BigDecimal("300"))
                        )
                ),
                null,
                null
        );
        givenJobCatalog(
                List.of(HERO, PHANTOM),
                Map.of("히어로", HERO)
        );

        GetJobStatisticsResult result = service.getJobStatistics();

        assertThat(result.jobs())
                .extracting(
                        job -> job.jobSlug(),
                        job -> job.count(),
                        job -> job.averageLevel()
                )
                .containsExactly(
                        tuple("hero", 5L, new BigDecimal("200.0")),
                        tuple("phantom", 0L, null)
                );
    }

    @Test
    void 최신과이전Snapshot의원본이름을한번에해석한다() {
        StatisticsQueryService service = createService();

        givenJobComparison(
                jobSnapshot(
                        SNAPSHOT_DATE,
                        COLLECTED_AT,
                        10,
                        List.of(
                                new JobCount("히어로", 10L, new BigDecimal("200"))
                        )
                ),
                jobSnapshot(
                        PREVIOUS_SNAPSHOT_DATE,
                        PREVIOUS_COLLECTED_AT,
                        10,
                        List.of(
                                new JobCount("팬텀", 10L, new BigDecimal("199"))
                        )
                ),
                3
        );
        givenJobCatalog(
                List.of(HERO, PHANTOM),
                Map.of("히어로", HERO, "팬텀", PHANTOM)
        );

        service.getJobStatistics();

        verify(jobCatalogQuery).resolveAliases(aliasNamesCaptor.capture());

        assertThat(aliasNamesCaptor.getValue())
                .containsExactlyInAnyOrder("히어로", "팬텀");
    }

    @Test
    void 이전Snapshot이있으면점유율차이로changeRate를계산한다() {
        StatisticsQueryService service = createService();

        givenJobComparison(
                jobSnapshot(
                        SNAPSHOT_DATE,
                        COLLECTED_AT,
                        1000,
                        List.of(
                                new JobCount("히어로", 124L, new BigDecimal("200")),
                                new JobCount("팬텀", 80L, new BigDecimal("200"))
                        )
                ),
                jobSnapshot(
                        PREVIOUS_SNAPSHOT_DATE,
                        PREVIOUS_COLLECTED_AT,
                        500,
                        List.of(
                                new JobCount("히어로", 58L, new BigDecimal("199")),
                                new JobCount("팬텀", 45L, new BigDecimal("199"))
                        )
                ),
                3
        );
        givenJobCatalog(
                List.of(HERO, PHANTOM),
                Map.of("히어로", HERO, "팬텀", PHANTOM)
        );

        GetJobStatisticsResult result = service.getJobStatistics();

        assertThat(result.previousAsOf()).isEqualTo(PREVIOUS_SNAPSHOT_DATE);
        assertThat(result.daysBetween()).isEqualTo(3);
        assertThat(result.jobs())
                .extracting(
                        job -> job.jobSlug(),
                        job -> job.percentage(),
                        job -> job.changeRate()
                )
                .containsExactly(
                        tuple(
                                "hero",
                                new BigDecimal("12.40"),
                                new BigDecimal("0.80")
                        ),
                        tuple(
                                "phantom",
                                new BigDecimal("8.00"),
                                new BigDecimal("-1.00")
                        )
                );
    }

    @Test
    void changeRate는반올림전원본비율의차이를한번반올림한값이다() {
        StatisticsQueryService service = createService();

        givenJobComparison(
                jobSnapshot(
                        SNAPSHOT_DATE,
                        COLLECTED_AT,
                        3,
                        List.of(
                                new JobCount("히어로", 1L, new BigDecimal("200"))
                        )
                ),
                jobSnapshot(
                        PREVIOUS_SNAPSHOT_DATE,
                        PREVIOUS_COLLECTED_AT,
                        7,
                        List.of(
                                new JobCount("히어로", 1L, new BigDecimal("200"))
                        )
                ),
                3
        );
        givenJobCatalog(List.of(HERO), Map.of("히어로", HERO));

        GetJobStatisticsResult result = service.getJobStatistics();

        assertThat(result.jobs()).hasSize(1);

        BigDecimal percentage = result.jobs().get(0).percentage();
        BigDecimal changeRate = result.jobs().get(0).changeRate();

        assertThat(percentage).isEqualTo(new BigDecimal("33.33"));
        assertThat(changeRate)
                .isEqualTo(new BigDecimal("19.05"))
                .isNotEqualTo(percentage.subtract(new BigDecimal("14.29")));
        assertThat(result.jobs().get(0).comparison().percentagePointChange())
                .isEqualTo(changeRate);
    }

    @Test
    void 이전Snapshot이없으면changeRate와비교메타데이터가없다() {
        StatisticsQueryService service = createService();

        givenJobComparison(
                jobSnapshot(
                        SNAPSHOT_DATE,
                        COLLECTED_AT,
                        2,
                        List.of(
                                new JobCount("히어로", 2L, new BigDecimal("200"))
                        )
                ),
                null,
                null
        );
        givenJobCatalog(List.of(HERO), Map.of("히어로", HERO));

        GetJobStatisticsResult result = service.getJobStatistics();

        assertThat(result.previousAsOf()).isNull();
        assertThat(result.daysBetween()).isNull();
        assertThat(result.jobs())
                .extracting(job -> job.changeRate())
                .containsOnlyNulls();
    }

    @Test
    void 이전sampleSize가0이면changeRate가null이다() {
        StatisticsQueryService service = createService();

        givenJobComparison(
                jobSnapshot(
                        SNAPSHOT_DATE,
                        COLLECTED_AT,
                        2,
                        List.of(
                                new JobCount("히어로", 2L, new BigDecimal("200"))
                        )
                ),
                jobSnapshot(
                        PREVIOUS_SNAPSHOT_DATE,
                        PREVIOUS_COLLECTED_AT,
                        0,
                        List.of()
                ),
                3
        );
        givenJobCatalog(List.of(HERO), Map.of("히어로", HERO));

        GetJobStatisticsResult result = service.getJobStatistics();

        assertThat(result.previousAsOf()).isEqualTo(PREVIOUS_SNAPSHOT_DATE);
        assertThat(result.jobs())
                .extracting(job -> job.changeRate())
                .containsOnlyNulls();
    }

    @Test
    void 이전에없던직업의changeRate는최신점유율과같다() {
        StatisticsQueryService service = createService();

        givenJobComparison(
                jobSnapshot(
                        SNAPSHOT_DATE,
                        COLLECTED_AT,
                        4,
                        List.of(
                                new JobCount("히어로", 3L, new BigDecimal("200")),
                                new JobCount("아델", 1L, new BigDecimal("200"))
                        )
                ),
                jobSnapshot(
                        PREVIOUS_SNAPSHOT_DATE,
                        PREVIOUS_COLLECTED_AT,
                        4,
                        List.of(
                                new JobCount("히어로", 4L, new BigDecimal("199"))
                        )
                ),
                3
        );
        givenJobCatalog(
                List.of(HERO, ADELE),
                Map.of("히어로", HERO, "아델", ADELE)
        );

        GetJobStatisticsResult result = service.getJobStatistics();

        assertThat(result.jobs())
                .extracting(
                        job -> job.jobSlug(),
                        job -> job.changeRate()
                )
                .containsExactly(
                        tuple("hero", new BigDecimal("-25.00")),
                        tuple("adele", new BigDecimal("25.00"))
                );
    }

    @Test
    void 이전에만있던직업도Canonical목록에있으면Count0으로반환한다() {
        StatisticsQueryService service = createService();

        givenJobComparison(
                jobSnapshot(
                        SNAPSHOT_DATE,
                        COLLECTED_AT,
                        2,
                        List.of(
                                new JobCount("히어로", 2L, new BigDecimal("200"))
                        )
                ),
                jobSnapshot(
                        PREVIOUS_SNAPSHOT_DATE,
                        PREVIOUS_COLLECTED_AT,
                        2,
                        List.of(
                                new JobCount("히어로", 1L, new BigDecimal("199")),
                                new JobCount("팬텀", 1L, new BigDecimal("199"))
                        )
                ),
                3
        );
        givenJobCatalog(
                List.of(HERO, PHANTOM),
                Map.of("히어로", HERO, "팬텀", PHANTOM)
        );

        GetJobStatisticsResult result = service.getJobStatistics();

        assertThat(result.jobs())
                .extracting(
                        job -> job.jobSlug(),
                        job -> job.count(),
                        job -> job.changeRate()
                )
                .containsExactly(
                        tuple("hero", 2L, new BigDecimal("50.00")),
                        tuple("phantom", 0L, new BigDecimal("-50.00"))
                );
    }

    @Test
    void sampleSize가0이면Percentage를0으로두고Canonical전체를반환한다() {
        StatisticsQueryService service = createService();

        givenJobComparison(
                jobSnapshot(
                        SNAPSHOT_DATE,
                        COLLECTED_AT,
                        0,
                        List.of()
                ),
                null,
                null
        );
        givenJobCatalog(List.of(HERO, PHANTOM), Map.of());

        GetJobStatisticsResult result = service.getJobStatistics();

        assertThat(result.sampleSize()).isEqualTo(0);
        assertThat(result.jobs())
                .extracting(
                        job -> job.jobSlug(),
                        job -> job.count(),
                        job -> job.percentage(),
                        job -> job.averageLevel()
                )
                .containsExactly(
                        tuple("hero", 0L, new BigDecimal("0.00"), null),
                        tuple("phantom", 0L, new BigDecimal("0.00"), null)
                );
    }

    @Test
    void 비교조회NOT_FOUND는직업통계실패로변환한다() {
        StatisticsQueryService service = createService();

        given(overallRankingComparisonQuery.getJobStatisticsComparison())
                .willThrow(new OverallRankingComparisonQueryException(
                        OverallRankingComparisonQueryFailure.NOT_FOUND
                ));

        JobStatisticsException exception =
                catchThrowableOfType(
                        service::getJobStatistics,
                        JobStatisticsException.class
                );

        assertThat(exception.getFailure())
                .isEqualTo(JobStatisticsFailure.SNAPSHOT_NOT_FOUND);
        assertThat(exception.getCause())
                .isInstanceOf(OverallRankingComparisonQueryException.class);
        verifyNoInteractions(jobCatalogQuery);
    }

    @Test
    void 비교조회DATA_INVALID는직업통계실패로변환한다() {
        StatisticsQueryService service = createService();

        given(overallRankingComparisonQuery.getJobStatisticsComparison())
                .willThrow(new OverallRankingComparisonQueryException(
                        OverallRankingComparisonQueryFailure.DATA_INVALID
                ));

        JobStatisticsException exception =
                catchThrowableOfType(
                        service::getJobStatistics,
                        JobStatisticsException.class
                );

        assertThat(exception.getFailure())
                .isEqualTo(JobStatisticsFailure.DATA_INVALID);
    }

    @Test
    void 직업통계조회는월드Query를호출하지않는다() {
        StatisticsQueryService service = createService();

        givenJobComparison(
                jobSnapshot(
                        SNAPSHOT_DATE,
                        COLLECTED_AT,
                        1,
                        List.of(
                                new JobCount("히어로", 1L, new BigDecimal("200"))
                        )
                ),
                null,
                null
        );
        givenJobCatalog(List.of(HERO), Map.of("히어로", HERO));

        service.getJobStatistics();

        verifyNoInteractions(overallRankingWorldStatisticsQuery);
        verifyNoInteractions(worldCatalogQuery);
    }

    @Test
    void 월드별통계를Slug와백분율평균레벨로변환한다() {
        StatisticsQueryService service = createService();

        givenWorldSnapshot(
                3,
                List.of(
                        new WorldCount("베라", 1L, new BigDecimal("210")),
                        new WorldCount("루나", 2L, new BigDecimal("200.55"))
                )
        );
        givenWorldCatalog(
                List.of(LUNA, BERA),
                Map.of("루나", LUNA, "베라", BERA)
        );

        GetWorldStatisticsResult result = service.getWorldStatistics();

        assertThat(result.sampleSize()).isEqualTo(3);
        assertThat(result.asOf()).isEqualTo(SNAPSHOT_DATE);
        assertThat(result.worlds())
                .extracting(
                        world -> world.worldSlug(),
                        world -> world.worldName(),
                        world -> world.percentage(),
                        world -> world.averageLevel()
                )
                .containsExactly(
                        tuple(
                                "luna",
                                "루나",
                                new BigDecimal("66.67"),
                                new BigDecimal("200.6")
                        ),
                        tuple(
                                "bera",
                                "베라",
                                new BigDecimal("33.33"),
                                new BigDecimal("210.0")
                        )
                );
    }

    @Test
    void 표본에없는Canonical월드는Count0과Percentage0과AverageLevel없음으로반환한다() {
        StatisticsQueryService service = createService();

        givenWorldSnapshot(
                2,
                List.of(new WorldCount("루나", 2L, new BigDecimal("200")))
        );
        givenWorldCatalog(List.of(LUNA, BERA), Map.of("루나", LUNA));

        GetWorldStatisticsResult result = service.getWorldStatistics();

        assertThat(result.worlds())
                .extracting(
                        world -> world.worldSlug(),
                        world -> world.count(),
                        world -> world.percentage(),
                        world -> world.averageLevel()
                )
                .containsExactly(
                        tuple(
                                "luna",
                                2L,
                                new BigDecimal("100.00"),
                                new BigDecimal("200.0")
                        ),
                        tuple("bera", 0L, new BigDecimal("0.00"), null)
                );
    }

    @Test
    void 같은Canonical월드에매칭된Alias는Count합산과가중평균으로계산한다() {
        StatisticsQueryService service = createService();

        givenWorldSnapshot(
                1001,
                List.of(
                        new WorldCount("루나", 1000L, new BigDecimal("200")),
                        new WorldCount("Luna", 1L, new BigDecimal("10"))
                )
        );
        givenWorldCatalog(
                List.of(LUNA),
                Map.of("루나", LUNA, "Luna", LUNA)
        );

        GetWorldStatisticsResult result = service.getWorldStatistics();

        assertThat(result.worlds()).hasSize(1);
        assertThat(result.worlds().get(0).count()).isEqualTo(1001L);
        assertThat(result.worlds().get(0).averageLevel())
                .isEqualTo(new BigDecimal("199.8"));
    }

    @Test
    void Alias미매칭월드Row는어떤Canonical월드에도포함하지않는다() {
        StatisticsQueryService service = createService();

        givenWorldSnapshot(
                10,
                List.of(
                        new WorldCount("루나", 5L, new BigDecimal("200")),
                        new WorldCount("등록되지않은월드", 5L, new BigDecimal("300"))
                )
        );
        givenWorldCatalog(List.of(LUNA, BERA), Map.of("루나", LUNA));

        GetWorldStatisticsResult result = service.getWorldStatistics();

        assertThat(result.worlds())
                .extracting(
                        world -> world.worldSlug(),
                        world -> world.count()
                )
                .containsExactly(
                        tuple("luna", 5L),
                        tuple("bera", 0L)
                );
    }

    @Test
    void 월드별sampleSize가0이면Percentage를0으로두고Canonical전체를반환한다() {
        StatisticsQueryService service = createService();

        givenWorldSnapshot(0, List.of());
        givenWorldCatalog(List.of(LUNA, BERA), Map.of());

        GetWorldStatisticsResult result = service.getWorldStatistics();

        assertThat(result.sampleSize()).isEqualTo(0);
        assertThat(result.worlds())
                .extracting(
                        world -> world.worldSlug(),
                        world -> world.count(),
                        world -> world.percentage()
                )
                .containsExactly(
                        tuple("luna", 0L, new BigDecimal("0.00")),
                        tuple("bera", 0L, new BigDecimal("0.00"))
                );
    }

    @Test
    void 월드별ranking조회실패는변환없이그대로전파한다() {
        StatisticsQueryService service = createService();

        given(overallRankingWorldStatisticsQuery.getLatestWorldStatistics())
                .willThrow(new OverallRankingWorldStatisticsQueryException(
                        OverallRankingWorldStatisticsQueryFailure.NOT_FOUND
                ));

        OverallRankingWorldStatisticsQueryException exception =
                catchThrowableOfType(
                        service::getWorldStatistics,
                        OverallRankingWorldStatisticsQueryException.class
                );

        assertThat(exception.getFailure())
                .isEqualTo(OverallRankingWorldStatisticsQueryFailure.NOT_FOUND);
        verifyNoInteractions(worldCatalogQuery);
    }

    private StatisticsQueryService createService() {
        return new StatisticsQueryService(
                overallRankingComparisonQuery,
                overallRankingWorldStatisticsQuery,
                jobCatalogQuery,
                worldCatalogQuery
        );
    }

    private void givenJobComparison(
            OverallRankingStatisticsSnapshot latest,
            OverallRankingStatisticsSnapshot previous,
            Integer daysBetween
    ) {
        given(overallRankingComparisonQuery.getJobStatisticsComparison())
                .willReturn(new OverallRankingStatisticsComparisonSnapshot(
                        latest,
                        previous,
                        daysBetween
                ));
    }

    private void givenJobCatalog(
            List<CanonicalJob> canonicalJobs,
            Map<String, CanonicalJob> canonicalJobsByClassName
    ) {
        given(jobCatalogQuery.findAll()).willReturn(canonicalJobs);
        given(jobCatalogQuery.resolveAliases(any()))
                .willReturn(canonicalJobsByClassName);
    }

    private void givenWorldSnapshot(
            int sampleSize,
            List<WorldCount> worldCounts
    ) {
        given(overallRankingWorldStatisticsQuery.getLatestWorldStatistics())
                .willReturn(new OverallRankingWorldStatisticsSnapshot(
                        SNAPSHOT_DATE,
                        "NEXON_OPEN_API",
                        COLLECTED_AT,
                        sampleSize,
                        1,
                        100,
                        false,
                        worldCounts
                ));
    }

    private void givenWorldCatalog(
            List<CanonicalWorld> canonicalWorlds,
            Map<String, CanonicalWorld> canonicalWorldsByWorldName
    ) {
        given(worldCatalogQuery.findAll()).willReturn(canonicalWorlds);
        given(worldCatalogQuery.resolveAliases(any()))
                .willReturn(canonicalWorldsByWorldName);
    }

    private OverallRankingStatisticsSnapshot jobSnapshot(
            LocalDate asOf,
            Instant collectedAt,
            int sampleSize,
            List<JobCount> jobCounts
    ) {
        return new OverallRankingStatisticsSnapshot(
                asOf,
                "NEXON_OPEN_API",
                collectedAt,
                sampleSize,
                1,
                100,
                false,
                jobCounts
        );
    }
}
