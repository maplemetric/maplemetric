package com.maplemetric.statistics.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

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
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    @Mock
    private OverallRankingComparisonQuery overallRankingComparisonQuery;

    @Mock
    private OverallRankingWorldStatisticsQuery overallRankingWorldStatisticsQuery;

    @Test
    void 직업별통계를백분율과평균레벨로변환한다() {
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

        GetJobStatisticsResult result = service.getJobStatistics();

        assertThat(result.sampleSize()).isEqualTo(3);
        assertThat(result.asOf()).isEqualTo(SNAPSHOT_DATE);
        assertThat(result.jobs())
                .extracting(
                        job -> job.jobName(),
                        job -> job.percentage(),
                        job -> job.averageLevel()
                )
                .containsExactlyInAnyOrder(
                        tuple(
                                "히어로",
                                new BigDecimal("33.33"),
                                new BigDecimal("200.5")
                        ),
                        tuple(
                                "팬텀",
                                new BigDecimal("66.67"),
                                new BigDecimal("204.7")
                        )
                );
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

        GetJobStatisticsResult result = service.getJobStatistics();

        assertThat(result.previousAsOf()).isEqualTo(PREVIOUS_SNAPSHOT_DATE);
        assertThat(result.daysBetween()).isEqualTo(3);
        assertThat(result.jobs())
                .extracting(
                        job -> job.jobName(),
                        job -> job.percentage(),
                        job -> job.changeRate()
                )
                .containsExactly(
                        tuple(
                                "히어로",
                                new BigDecimal("12.40"),
                                new BigDecimal("0.80")
                        ),
                        tuple(
                                "팬텀",
                                new BigDecimal("8.00"),
                                new BigDecimal("-1.00")
                        )
                );
    }

    @Test
    void changeRate는반올림된percentage끼리의차이와일치한다() {
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

        GetJobStatisticsResult result = service.getJobStatistics();

        assertThat(result.jobs()).hasSize(1);

        BigDecimal percentage = result.jobs().get(0).percentage();
        BigDecimal changeRate = result.jobs().get(0).changeRate();

        assertThat(percentage).isEqualTo(new BigDecimal("33.33"));
        assertThat(changeRate)
                .isEqualTo(percentage.subtract(new BigDecimal("14.29")));
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

        GetJobStatisticsResult result = service.getJobStatistics();

        assertThat(result.jobs())
                .extracting(
                        job -> job.jobName(),
                        job -> job.changeRate()
                )
                .containsExactly(
                        tuple("히어로", new BigDecimal("-25.00")),
                        tuple("아델", new BigDecimal("25.00"))
                );
    }

    @Test
    void 이전에만있던직업은응답목록에포함하지않는다() {
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

        GetJobStatisticsResult result = service.getJobStatistics();

        assertThat(result.jobs())
                .extracting(job -> job.jobName())
                .containsExactly("히어로");
    }

    @Test
    void sampleSize가0이면percentage계산없이빈목록을반환한다() {
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

        GetJobStatisticsResult result = service.getJobStatistics();

        assertThat(result.sampleSize()).isEqualTo(0);
        assertThat(result.jobs()).isEmpty();
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
    void 직업통계조회는월드통계Query를호출하지않는다() {
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

        service.getJobStatistics();

        verifyNoInteractions(overallRankingWorldStatisticsQuery);
    }

    @Test
    void 월드별통계를백분율과평균레벨로변환하고순서를유지한다() {
        StatisticsQueryService service = createService();

        given(overallRankingWorldStatisticsQuery.getLatestWorldStatistics())
                .willReturn(
                        new OverallRankingWorldStatisticsSnapshot(
                                SNAPSHOT_DATE,
                                "NEXON_OPEN_API",
                                COLLECTED_AT,
                                3,
                                1,
                                100,
                                false,
                                List.of(
                                        new WorldCount(
                                                "베라",
                                                1L,
                                                new BigDecimal("210")
                                        ),
                                        new WorldCount(
                                                "루나",
                                                2L,
                                                new BigDecimal("200.55")
                                        )
                                )
                        )
                );

        GetWorldStatisticsResult result = service.getWorldStatistics();

        assertThat(result.sampleSize()).isEqualTo(3);
        assertThat(result.asOf()).isEqualTo(SNAPSHOT_DATE);
        assertThat(result.worlds())
                .extracting(
                        world -> world.worldName(),
                        world -> world.percentage(),
                        world -> world.averageLevel()
                )
                .containsExactly(
                        tuple(
                                "베라",
                                new BigDecimal("33.33"),
                                new BigDecimal("210.0")
                        ),
                        tuple(
                                "루나",
                                new BigDecimal("66.67"),
                                new BigDecimal("200.6")
                        )
                );
    }

    @Test
    void 월드별sampleSize가0이면percentage계산없이빈목록을반환한다() {
        StatisticsQueryService service = createService();

        given(overallRankingWorldStatisticsQuery.getLatestWorldStatistics())
                .willReturn(
                        new OverallRankingWorldStatisticsSnapshot(
                                SNAPSHOT_DATE,
                                "NEXON_OPEN_API",
                                COLLECTED_AT,
                                0,
                                1,
                                100,
                                false,
                                List.of()
                        )
                );

        GetWorldStatisticsResult result = service.getWorldStatistics();

        assertThat(result.sampleSize()).isEqualTo(0);
        assertThat(result.worlds()).isEmpty();
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
    }

    private StatisticsQueryService createService() {
        return new StatisticsQueryService(
                overallRankingComparisonQuery,
                overallRankingWorldStatisticsQuery
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
