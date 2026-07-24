package com.maplemetric.statistics.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.BDDMockito.given;

import com.maplemetric.ranking.api.OverallRankingStatisticsQuery;
import com.maplemetric.ranking.api.OverallRankingStatisticsQueryException;
import com.maplemetric.ranking.api.OverallRankingStatisticsQueryFailure;
import com.maplemetric.ranking.api.OverallRankingStatisticsSnapshot;
import com.maplemetric.ranking.api.OverallRankingStatisticsSnapshot.JobCount;
import com.maplemetric.ranking.api.OverallRankingWorldStatisticsQuery;
import com.maplemetric.ranking.api.OverallRankingWorldStatisticsQueryException;
import com.maplemetric.ranking.api.OverallRankingWorldStatisticsQueryFailure;
import com.maplemetric.ranking.api.OverallRankingWorldStatisticsSnapshot;
import com.maplemetric.ranking.api.OverallRankingWorldStatisticsSnapshot.WorldCount;
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

    private static final Instant COLLECTED_AT =
            Instant.parse("2026-07-24T01:00:00Z");

    @Mock
    private OverallRankingStatisticsQuery overallRankingStatisticsQuery;

    @Mock
    private OverallRankingWorldStatisticsQuery overallRankingWorldStatisticsQuery;

    @Test
    void 직업별통계를백분율과평균레벨로변환한다() {
        StatisticsQueryService service = new StatisticsQueryService(
                overallRankingStatisticsQuery,
                overallRankingWorldStatisticsQuery
        );

        given(overallRankingStatisticsQuery.getLatestJobStatistics())
                .willReturn(
                        new OverallRankingStatisticsSnapshot(
                                SNAPSHOT_DATE,
                                "NEXON_OPEN_API",
                                COLLECTED_AT,
                                3,
                                1,
                                100,
                                false,
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
                        )
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
    void sampleSize가0이면percentage계산없이빈목록을반환한다() {
        StatisticsQueryService service = new StatisticsQueryService(
                overallRankingStatisticsQuery,
                overallRankingWorldStatisticsQuery
        );

        given(overallRankingStatisticsQuery.getLatestJobStatistics())
                .willReturn(
                        new OverallRankingStatisticsSnapshot(
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

        GetJobStatisticsResult result = service.getJobStatistics();

        assertThat(result.sampleSize()).isEqualTo(0);
        assertThat(result.jobs()).isEmpty();
    }

    @Test
    void ranking조회실패는변환없이그대로전파한다() {
        StatisticsQueryService service = new StatisticsQueryService(
                overallRankingStatisticsQuery,
                overallRankingWorldStatisticsQuery
        );

        given(overallRankingStatisticsQuery.getLatestJobStatistics())
                .willThrow(new OverallRankingStatisticsQueryException(
                        OverallRankingStatisticsQueryFailure.NOT_FOUND
                ));

        OverallRankingStatisticsQueryException exception =
                catchThrowableOfType(
                        service::getJobStatistics,
                        OverallRankingStatisticsQueryException.class
                );

        assertThat(exception.getFailure())
                .isEqualTo(OverallRankingStatisticsQueryFailure.NOT_FOUND);
    }

    @Test
    void 월드별통계를백분율과평균레벨로변환하고순서를유지한다() {
        StatisticsQueryService service = new StatisticsQueryService(
                overallRankingStatisticsQuery,
                overallRankingWorldStatisticsQuery
        );

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
        StatisticsQueryService service = new StatisticsQueryService(
                overallRankingStatisticsQuery,
                overallRankingWorldStatisticsQuery
        );

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
        StatisticsQueryService service = new StatisticsQueryService(
                overallRankingStatisticsQuery,
                overallRankingWorldStatisticsQuery
        );

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
}
