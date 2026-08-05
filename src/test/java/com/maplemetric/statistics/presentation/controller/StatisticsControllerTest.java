package com.maplemetric.statistics.presentation.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.maplemetric.ranking.api.OverallRankingComparisonQueryException;
import com.maplemetric.ranking.api.OverallRankingComparisonQueryFailure;
import com.maplemetric.ranking.api.OverallRankingWorldStatisticsQueryException;
import com.maplemetric.ranking.api.OverallRankingWorldStatisticsQueryFailure;
import com.maplemetric.statistics.application.exception.JobStatisticsException;
import com.maplemetric.statistics.application.exception.JobStatisticsFailure;
import com.maplemetric.statistics.application.exception.WorldStatisticsException;
import com.maplemetric.statistics.application.exception.WorldStatisticsFailure;
import com.maplemetric.statistics.application.result.GetWorldStatisticsDetailResult;
import com.maplemetric.statistics.application.result.GetWorldStatisticsHistoryResult;
import com.maplemetric.statistics.application.service.WorldStatisticsDetailQueryService;
import com.maplemetric.world.api.CanonicalWorld;
import com.maplemetric.statistics.application.result.GetJobStatisticsDetailResult;
import com.maplemetric.statistics.application.result.GetJobStatisticsDetailResult.ComparisonResult;
import com.maplemetric.statistics.application.result.GetJobStatisticsDetailResult.JobResult;
import com.maplemetric.statistics.application.result.GetJobStatisticsDetailResult.LatestResult;
import com.maplemetric.statistics.application.result.GetJobStatisticsDetailResult.SourceMetaResult;
import com.maplemetric.statistics.application.result.GetJobStatisticsHistoryResult;
import com.maplemetric.statistics.application.result.GetJobStatisticsHistoryResult.PointResult;
import com.maplemetric.statistics.application.result.GetJobStatisticsHistoryResult.RangeComparisonResult;
import com.maplemetric.statistics.application.result.GetJobStatisticsHistoryResult.RangeResult;
import com.maplemetric.statistics.application.result.GetJobStatisticsResult;
import com.maplemetric.statistics.application.result.GetJobStatisticsResult.JobComparisonResult;
import com.maplemetric.statistics.application.result.GetJobStatisticsResult.JobStatisticsResult;
import com.maplemetric.statistics.api.StatisticsTrend;
import com.maplemetric.statistics.application.result.GetWorldStatisticsResult;
import com.maplemetric.statistics.application.result.GetWorldStatisticsResult.WorldComparisonResult;
import com.maplemetric.statistics.application.result.GetWorldStatisticsResult.WorldStatisticsResult;
import com.maplemetric.statistics.api.StatisticsDataAvailability;
import com.maplemetric.statistics.application.service.JobStatisticsDetailQueryService;
import com.maplemetric.statistics.application.service.StatisticsQueryService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(StatisticsController.class)
class StatisticsControllerTest {

    private static final LocalDate SNAPSHOT_DATE =
            LocalDate.of(2026, 7, 24);

    private static final LocalDate PREVIOUS_SNAPSHOT_DATE =
            LocalDate.of(2026, 7, 21);

    private static final Instant COLLECTED_AT =
            Instant.parse("2026-07-24T01:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StatisticsQueryService statisticsQueryService;

    @MockitoBean
    private JobStatisticsDetailQueryService jobStatisticsDetailQueryService;

    @MockitoBean
    private WorldStatisticsDetailQueryService
            worldStatisticsDetailQueryService;

    @Test
    void 직업별통계응답을반환한다() throws Exception {
        given(statisticsQueryService.getJobStatistics())
                .willReturn(
                        new GetJobStatisticsResult(
                                List.of(
                                        new JobStatisticsResult(
                                                "hero",
                                                "히어로",
                                                320L,
                                                new BigDecimal("12.40"),
                                                new BigDecimal("187.3"),
                                                new BigDecimal("0.80"),
                                                new JobComparisonResult(
                                                        300L,
                                                        new BigDecimal("11.60"),
                                                        20L,
                                                        new BigDecimal("6.67"),
                                                        new BigDecimal("6.90"),
                                                        new BigDecimal("0.80"),
                                                        StatisticsTrend.UP
                                                )
                                        ),
                                        new JobStatisticsResult(
                                                "phantom",
                                                "팬텀",
                                                0L,
                                                new BigDecimal("0.00"),
                                                null,
                                                null,
                                                new JobComparisonResult(
                                                        0L,
                                                        new BigDecimal("0.00"),
                                                        null,
                                                        null,
                                                        null,
                                                        null,
                                                        StatisticsTrend.INSUFFICIENT_DATA
                                                )
                                        )
                                ),
                                2581,
                                SNAPSHOT_DATE,
                                "NEXON_OPEN_API",
                                COLLECTED_AT,
                                13,
                                100,
                                false,
                                PREVIOUS_SNAPSHOT_DATE,
                                3
                        )
                );

        mockMvc.perform(get("/api/v1/statistics/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(
                        jsonPath("$.code")
                                .value("JOB_STATISTICS_SEARCH_SUCCESS")
                )
                .andExpect(
                        jsonPath("$.data.jobs[0].changeRate")
                                .value(0.80)
                )
                .andExpect(
                        jsonPath("$.data.previousAsOf")
                                .value("2026-07-21")
                )
                .andExpect(
                        jsonPath("$.data.daysBetween")
                                .value(3)
                )
                .andExpect(
                        jsonPath("$.data.jobs[0].jobSlug")
                                .value("hero")
                )
                .andExpect(
                        jsonPath("$.data.jobs[0].jobName")
                                .value("히어로")
                )
                .andExpect(
                        jsonPath("$.data.jobs[0].count")
                                .value(320)
                )
                .andExpect(
                        jsonPath("$.data.jobs[1].jobSlug")
                                .value("phantom")
                )
                .andExpect(
                        jsonPath("$.data.jobs[1].count")
                                .value(0)
                )
                .andExpect(
                        jsonPath("$.data.jobs[1].percentage")
                                .value(0.00)
                )
                .andExpect(
                        jsonPath("$.data.jobs[1].averageLevel")
                                .isEmpty()
                )
                .andExpect(
                        jsonPath("$.data.jobs[0].comparison.previousCount")
                                .value(300)
                )
                .andExpect(
                        jsonPath("$.data.jobs[0].comparison.previousPercentage")
                                .value(11.60)
                )
                .andExpect(
                        jsonPath("$.data.jobs[0].comparison.countChange")
                                .value(20)
                )
                .andExpect(
                        jsonPath("$.data.jobs[0].comparison.countChangeRate")
                                .value(6.67)
                )
                .andExpect(
                        jsonPath(
                                "$.data.jobs[0].comparison.percentageChangeRate"
                        ).value(6.90)
                )
                .andExpect(
                        jsonPath(
                                "$.data.jobs[0].comparison.percentagePointChange"
                        ).value(0.80)
                )
                .andExpect(
                        jsonPath("$.data.jobs[0].comparison.trend")
                                .value("UP")
                )
                .andExpect(
                        jsonPath("$.data.jobs[1].comparison.trend")
                                .value("INSUFFICIENT_DATA")
                )
                .andExpect(
                        jsonPath(
                                "$.data.jobs[1].comparison.percentagePointChange"
                        ).isEmpty()
                )
                .andExpect(
                        jsonPath("$.data.jobs[0].percentage")
                                .value(12.40)
                )
                .andExpect(
                        jsonPath("$.data.jobs[0].averageLevel")
                                .value(187.3)
                )
                .andExpect(
                        jsonPath("$.data.sampleSize")
                                .value(2581)
                )
                .andExpect(
                        jsonPath("$.data.asOf")
                                .value("2026-07-24")
                )
                .andExpect(
                        jsonPath("$.data.source")
                                .value("NEXON_OPEN_API")
                )
                .andExpect(
                        jsonPath("$.data.collectedAt")
                                .value("2026-07-24T01:00:00Z")
                )
                .andExpect(
                        jsonPath("$.data.pageCount")
                                .value(13)
                )
                .andExpect(
                        jsonPath("$.data.requestedMaxPages")
                                .value(100)
                )
                .andExpect(
                        jsonPath("$.data.truncated")
                                .value(false)
                );
    }

    @Test
    void sampleSize가0이면빈jobs와메타데이터를정상반환한다() throws Exception {
        given(statisticsQueryService.getJobStatistics())
                .willReturn(
                        new GetJobStatisticsResult(
                                List.of(),
                                0,
                                SNAPSHOT_DATE,
                                "NEXON_OPEN_API",
                                COLLECTED_AT,
                                1,
                                100,
                                false,
                                null,
                                null
                        )
                );

        mockMvc.perform(get("/api/v1/statistics/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(
                        jsonPath("$.data.sampleSize")
                                .value(0)
                )
                .andExpect(
                        jsonPath("$.data.previousAsOf")
                                .isEmpty()
                )
                .andExpect(
                        jsonPath("$.data.daysBetween")
                                .isEmpty()
                )
                .andExpect(
                        jsonPath("$.data.jobs")
                                .isArray()
                )
                .andExpect(
                        jsonPath("$.data.jobs")
                                .isEmpty()
                )
                .andExpect(
                        jsonPath("$.data.asOf")
                                .value("2026-07-24")
                )
                .andExpect(
                        jsonPath("$.data.source")
                                .value("NEXON_OPEN_API")
                )
                .andExpect(
                        jsonPath("$.data.collectedAt")
                                .value("2026-07-24T01:00:00Z")
                )
                .andExpect(
                        jsonPath("$.data.pageCount")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.data.requestedMaxPages")
                                .value(100)
                )
                .andExpect(
                        jsonPath("$.data.truncated")
                                .value(false)
                );
    }

    @Test
    void 이전Snapshot이없으면changeRate가null인응답을반환한다() throws Exception {
        given(statisticsQueryService.getJobStatistics())
                .willReturn(
                        new GetJobStatisticsResult(
                                List.of(
                                        new JobStatisticsResult(
                                                "hero",
                                                "히어로",
                                                320L,
                                                new BigDecimal("12.40"),
                                                new BigDecimal("187.3"),
                                                null,
                                                new JobComparisonResult(
                                                        null,
                                                        null,
                                                        null,
                                                        null,
                                                        null,
                                                        null,
                                                        StatisticsTrend.INSUFFICIENT_DATA
                                                )
                                        )
                                ),
                                2581,
                                SNAPSHOT_DATE,
                                "NEXON_OPEN_API",
                                COLLECTED_AT,
                                13,
                                100,
                                false,
                                null,
                                null
                        )
                );

        mockMvc.perform(get("/api/v1/statistics/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(
                        jsonPath("$.data.jobs[0].percentage")
                                .value(12.40)
                )
                .andExpect(
                        jsonPath("$.data.jobs[0].changeRate")
                                .isEmpty()
                )
                .andExpect(
                        jsonPath("$.data.jobs[0].comparison")
                                .exists()
                )
                .andExpect(
                        jsonPath("$.data.jobs[0].comparison.previousCount")
                                .isEmpty()
                )
                .andExpect(
                        jsonPath("$.data.jobs[0].comparison.previousPercentage")
                                .isEmpty()
                )
                .andExpect(
                        jsonPath("$.data.jobs[0].comparison.countChange")
                                .isEmpty()
                )
                .andExpect(
                        jsonPath("$.data.jobs[0].comparison.countChangeRate")
                                .isEmpty()
                )
                .andExpect(
                        jsonPath(
                                "$.data.jobs[0].comparison.percentageChangeRate"
                        ).isEmpty()
                )
                .andExpect(
                        jsonPath(
                                "$.data.jobs[0].comparison.percentagePointChange"
                        ).isEmpty()
                )
                .andExpect(
                        jsonPath("$.data.jobs[0].comparison.trend")
                                .value("INSUFFICIENT_DATA")
                )
                .andExpect(
                        jsonPath("$.data.previousAsOf")
                                .isEmpty()
                )
                .andExpect(
                        jsonPath("$.data.daysBetween")
                                .isEmpty()
                );
    }

    @Test
    void Snapshot이없으면404를반환한다() throws Exception {
        given(statisticsQueryService.getJobStatistics())
                .willThrow(new JobStatisticsException(
                        JobStatisticsFailure.SNAPSHOT_NOT_FOUND,
                        new OverallRankingComparisonQueryException(
                                OverallRankingComparisonQueryFailure.NOT_FOUND
                        )
                ));

        mockMvc.perform(get("/api/v1/statistics/jobs"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(
                        jsonPath("$.code").value("STATISTICS_001")
                );
    }

    @Test
    void 집계데이터가정합하지않으면500을반환한다() throws Exception {
        given(statisticsQueryService.getJobStatistics())
                .willThrow(new JobStatisticsException(
                        JobStatisticsFailure.DATA_INVALID,
                        new OverallRankingComparisonQueryException(
                                OverallRankingComparisonQueryFailure.DATA_INVALID
                        )
                ));

        mockMvc.perform(get("/api/v1/statistics/jobs"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(
                        jsonPath("$.code").value("STATISTICS_002")
                );
    }

    @Test
    void 월드별통계응답을반환한다() throws Exception {
        given(statisticsQueryService.getWorldStatistics())
                .willReturn(
                        new GetWorldStatisticsResult(
                                List.of(
                                        new WorldStatisticsResult(
                                                "luna",
                                                "루나",
                                                320L,
                                                new BigDecimal("12.40"),
                                                new BigDecimal("187.3"),
                                                new WorldComparisonResult(
                                                        305L,
                                                        new BigDecimal("11.65"),
                                                        15L,
                                                        new BigDecimal("4.92"),
                                                        new BigDecimal("6.44"),
                                                        new BigDecimal("0.75"),
                                                        StatisticsTrend.UP
                                                )
                                        ),
                                        new WorldStatisticsResult(
                                                "bera",
                                                "베라",
                                                0L,
                                                new BigDecimal("0.00"),
                                                null,
                                                new WorldComparisonResult(
                                                        0L,
                                                        new BigDecimal("0.00"),
                                                        0L,
                                                        null,
                                                        null,
                                                        new BigDecimal("0.00"),
                                                        StatisticsTrend.STABLE
                                                )
                                        )
                                ),
                                2581,
                                SNAPSHOT_DATE,
                                "NEXON_OPEN_API",
                                COLLECTED_AT,
                                13,
                                100,
                                false,
                                PREVIOUS_SNAPSHOT_DATE,
                                3
                        )
                );

        mockMvc.perform(get("/api/v1/statistics/worlds"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(
                        jsonPath("$.code")
                                .value("WORLD_STATISTICS_SEARCH_SUCCESS")
                )
                .andExpect(
                        jsonPath("$.data.worlds[0].worldSlug")
                                .value("luna")
                )
                .andExpect(
                        jsonPath("$.data.worlds[0].worldName")
                                .value("루나")
                )
                .andExpect(
                        jsonPath("$.data.worlds[0].count")
                                .value(320)
                )
                .andExpect(
                        jsonPath("$.data.worlds[1].worldSlug")
                                .value("bera")
                )
                .andExpect(
                        jsonPath("$.data.worlds[1].count")
                                .value(0)
                )
                .andExpect(
                        jsonPath("$.data.worlds[1].percentage")
                                .value(0.00)
                )
                .andExpect(
                        jsonPath("$.data.worlds[1].averageLevel")
                                .isEmpty()
                )
                .andExpect(
                        jsonPath("$.data.worlds[0].percentage")
                                .value(12.40)
                )
                .andExpect(
                        jsonPath("$.data.worlds[0].averageLevel")
                                .value(187.3)
                )
                .andExpect(
                        jsonPath(
                                "$.data.worlds[0].comparison.previousCount"
                        ).value(305)
                )
                .andExpect(
                        jsonPath(
                                "$.data.worlds[0].comparison"
                                        + ".percentagePointChange"
                        ).value(0.75)
                )
                .andExpect(
                        jsonPath("$.data.worlds[0].comparison.trend")
                                .value("UP")
                )
                .andExpect(
                        jsonPath("$.data.worlds[1].comparison.trend")
                                .value("STABLE")
                )
                .andExpect(
                        jsonPath("$.data.previousAsOf")
                                .value("2026-07-21")
                )
                .andExpect(jsonPath("$.data.daysBetween").value(3))
                .andExpect(
                        jsonPath("$.data.sampleSize")
                                .value(2581)
                )
                .andExpect(
                        jsonPath("$.data.asOf")
                                .value("2026-07-24")
                )
                .andExpect(
                        jsonPath("$.data.source")
                                .value("NEXON_OPEN_API")
                )
                .andExpect(
                        jsonPath("$.data.collectedAt")
                                .value("2026-07-24T01:00:00Z")
                )
                .andExpect(
                        jsonPath("$.data.pageCount")
                                .value(13)
                )
                .andExpect(
                        jsonPath("$.data.requestedMaxPages")
                                .value(100)
                )
                .andExpect(
                        jsonPath("$.data.truncated")
                                .value(false)
                );
    }

    @Test
    void 월드별sampleSize가0이면빈worlds와메타데이터를정상반환한다() throws Exception {
        given(statisticsQueryService.getWorldStatistics())
                .willReturn(
                        new GetWorldStatisticsResult(
                                List.of(),
                                0,
                                SNAPSHOT_DATE,
                                "NEXON_OPEN_API",
                                COLLECTED_AT,
                                1,
                                100,
                                false,
                                null,
                                null
                        )
                );

        mockMvc.perform(get("/api/v1/statistics/worlds"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(
                        jsonPath("$.data.sampleSize")
                                .value(0)
                )
                .andExpect(
                        jsonPath("$.data.worlds")
                                .isArray()
                )
                .andExpect(
                        jsonPath("$.data.worlds")
                                .isEmpty()
                );
    }

    @Test
    void 월드별Snapshot이없으면404를반환한다() throws Exception {
        given(statisticsQueryService.getWorldStatistics())
                .willThrow(new OverallRankingWorldStatisticsQueryException(
                        OverallRankingWorldStatisticsQueryFailure.NOT_FOUND
                ));

        mockMvc.perform(get("/api/v1/statistics/worlds"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(
                        jsonPath("$.code").value("STATISTICS_003")
                );
    }

    @Test
    void 월드별집계데이터가정합하지않으면500을반환한다() throws Exception {
        given(statisticsQueryService.getWorldStatistics())
                .willThrow(new OverallRankingWorldStatisticsQueryException(
                        OverallRankingWorldStatisticsQueryFailure.DATA_INVALID
                ));

        mockMvc.perform(get("/api/v1/statistics/worlds"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(
                        jsonPath("$.code").value("STATISTICS_004")
                );
    }

    @Test
    void 직업상세통계응답을반환한다() throws Exception {
        given(jobStatisticsDetailQueryService
                .getJobStatisticsDetail("hero"))
                .willReturn(new GetJobStatisticsDetailResult(
                        new JobResult(
                                "hero",
                                "히어로",
                                "모험가",
                                "전사",
                                true
                        ),
                        StatisticsDataAvailability.AVAILABLE,
                        new LatestResult(
                                SNAPSHOT_DATE,
                                320L,
                                new BigDecimal("12.40"),
                                new BigDecimal("287.3")
                        ),
                        new ComparisonResult(
                                PREVIOUS_SNAPSHOT_DATE,
                                300L,
                                20L,
                                new BigDecimal("6.67"),
                                new BigDecimal("11.60"),
                                new BigDecimal("6.90"),
                                new BigDecimal("0.80"),
                                StatisticsTrend.UP
                        ),
                        new SourceMetaResult(
                                "NEXON_OPEN_API",
                                COLLECTED_AT,
                                2581,
                                13,
                                100,
                                false
                        )
                ));

        mockMvc.perform(get("/api/v1/statistics/jobs/hero"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(
                        "JOB_STATISTICS_DETAIL_SEARCH_SUCCESS"
                ))
                .andExpect(jsonPath("$.data.job.jobSlug").value("hero"))
                .andExpect(jsonPath("$.data.job.jobName").value("히어로"))
                .andExpect(jsonPath("$.data.job.jobGroup").value("모험가"))
                .andExpect(jsonPath("$.data.job.jobBranch").value("전사"))
                .andExpect(jsonPath("$.data.job.available").value(true))
                .andExpect(jsonPath("$.data.dataAvailability")
                        .value("AVAILABLE"))
                .andExpect(jsonPath("$.data.latest.asOf")
                        .value("2026-07-24"))
                .andExpect(jsonPath("$.data.latest.count").value(320))
                .andExpect(jsonPath("$.data.latest.percentage").value(12.40))
                .andExpect(jsonPath("$.data.latest.averageLevel").value(287.3))
                .andExpect(jsonPath("$.data.comparison.previousAsOf")
                        .value("2026-07-21"))
                .andExpect(jsonPath("$.data.comparison.countChangeRate")
                        .value(6.67))
                .andExpect(jsonPath(
                        "$.data.comparison.percentageChangeRate"
                ).value(6.90))
                .andExpect(jsonPath(
                        "$.data.comparison.percentagePointChange"
                ).value(0.80))
                .andExpect(jsonPath("$.data.comparison.trend").value("UP"))
                .andExpect(jsonPath("$.data.sourceMeta.sampleSize")
                        .value(2581));
    }

    @Test
    void 정상직업이지만수집이력이없으면상세를200으로반환한다()
            throws Exception {
        given(jobStatisticsDetailQueryService
                .getJobStatisticsDetail("hero"))
                .willReturn(new GetJobStatisticsDetailResult(
                        new JobResult(
                                "hero",
                                "히어로",
                                "모험가",
                                "전사",
                                true
                        ),
                        StatisticsDataAvailability.NOT_COLLECTED,
                        null,
                        null,
                        null
                ));

        mockMvc.perform(get("/api/v1/statistics/jobs/hero"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dataAvailability")
                        .value("NOT_COLLECTED"))
                .andExpect(jsonPath("$.data.latest").isEmpty())
                .andExpect(jsonPath("$.data.comparison").isEmpty())
                .andExpect(jsonPath("$.data.sourceMeta").isEmpty());
    }

    @Test
    void 직업History응답을반환한다() throws Exception {
        given(jobStatisticsDetailQueryService.getJobStatisticsHistory(
                "hero",
                "7D",
                null,
                null
        )).willReturn(new GetJobStatisticsHistoryResult(
                new GetJobStatisticsHistoryResult.JobResult(
                        "hero",
                        "히어로"
                ),
                StatisticsDataAvailability.AVAILABLE,
                new RangeResult(
                        "7D",
                        LocalDate.of(2026, 7, 18),
                        SNAPSHOT_DATE,
                        LocalDate.of(2026, 7, 18),
                        SNAPSHOT_DATE,
                        6,
                        1
                ),
                new RangeComparisonResult(
                        LocalDate.of(2026, 7, 18),
                        SNAPSHOT_DATE,
                        280L,
                        320L,
                        40L,
                        new BigDecimal("14.29"),
                        new BigDecimal("10.80"),
                        new BigDecimal("12.40"),
                        new BigDecimal("14.81"),
                        new BigDecimal("1.60")
                ),
                List.of(new PointResult(
                        LocalDate.of(2026, 7, 18),
                        280L,
                        new BigDecimal("10.80"),
                        new BigDecimal("284.1"),
                        2592,
                        13,
                        100,
                        false,
                        COLLECTED_AT
                )),
                List.of()
        ));

        mockMvc.perform(get("/api/v1/statistics/jobs/hero/history")
                        .param("range", "7D"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(
                        "JOB_STATISTICS_HISTORY_SEARCH_SUCCESS"
                ))
                .andExpect(jsonPath("$.data.job.jobSlug").value("hero"))
                .andExpect(jsonPath("$.data.dataAvailability")
                        .value("AVAILABLE"))
                .andExpect(jsonPath("$.data.range.preset").value("7D"))
                .andExpect(jsonPath("$.data.range.requestedFrom")
                        .value("2026-07-18"))
                .andExpect(jsonPath("$.data.range.pointCount").value(6))
                .andExpect(jsonPath("$.data.range.missingDateCount").value(1))
                .andExpect(jsonPath("$.data.rangeComparison.countChangeRate")
                        .value(14.29))
                .andExpect(jsonPath(
                        "$.data.rangeComparison.percentageChangeRate"
                ).value(14.81))
                .andExpect(jsonPath(
                        "$.data.rangeComparison.percentagePointChange"
                ).value(1.60))
                .andExpect(jsonPath("$.data.points[0].asOf")
                        .value("2026-07-18"))
                .andExpect(jsonPath("$.data.points[0].sampleSize")
                        .value(2592))
                .andExpect(jsonPath("$.data.limitations").isEmpty());
    }

    @Test
    void CustomHistory날짜를파싱하고미수집Range를반환한다()
            throws Exception {
        LocalDate from = LocalDate.of(2026, 7, 1);
        LocalDate to = LocalDate.of(2026, 7, 7);

        given(jobStatisticsDetailQueryService.getJobStatisticsHistory(
                "hero",
                null,
                from,
                to
        )).willReturn(new GetJobStatisticsHistoryResult(
                new GetJobStatisticsHistoryResult.JobResult(
                        "hero",
                        "히어로"
                ),
                StatisticsDataAvailability.NOT_COLLECTED,
                new RangeResult(
                        null,
                        from,
                        to,
                        null,
                        null,
                        0,
                        7
                ),
                null,
                List.of(),
                List.of()
        ));

        mockMvc.perform(get("/api/v1/statistics/jobs/hero/history")
                        .param("from", "2026-07-01")
                        .param("to", "2026-07-07"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dataAvailability")
                        .value("NOT_COLLECTED"))
                .andExpect(jsonPath("$.data.range.preset").isEmpty())
                .andExpect(jsonPath("$.data.range.requestedFrom")
                        .value("2026-07-01"))
                .andExpect(jsonPath("$.data.range.requestedTo")
                        .value("2026-07-07"))
                .andExpect(jsonPath("$.data.range.pointCount").value(0))
                .andExpect(jsonPath("$.data.range.missingDateCount").value(7))
                .andExpect(jsonPath("$.data.rangeComparison").isEmpty())
                .andExpect(jsonPath("$.data.points").isEmpty());

        then(jobStatisticsDetailQueryService).should()
                .getJobStatisticsHistory("hero", null, from, to);
    }

    @Test
    void 알수없는직업Slug면404를반환한다() throws Exception {
        given(jobStatisticsDetailQueryService
                .getJobStatisticsDetail("unknown"))
                .willThrow(new JobStatisticsException(
                        JobStatisticsFailure.JOB_NOT_FOUND
                ));

        mockMvc.perform(get("/api/v1/statistics/jobs/unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("STATISTICS_005"));
    }

    @Test
    void 잘못된History기간요청이면400을반환한다() throws Exception {
        given(jobStatisticsDetailQueryService.getJobStatisticsHistory(
                "hero",
                "5D",
                null,
                null
        )).willThrow(new JobStatisticsException(
                JobStatisticsFailure.INVALID_HISTORY_REQUEST
        ));

        mockMvc.perform(get("/api/v1/statistics/jobs/hero/history")
                        .param("range", "5D"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("STATISTICS_006"));
    }

    @Test
    void malformedHistoryDateReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/statistics/jobs/hero/history")
                        .param("from", "2026-07-XX")
                        .param("to", "2026-07-07"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("GLOBAL_001"));

        then(jobStatisticsDetailQueryService).shouldHaveNoInteractions();
    }

    @Test
    void 월드상세통계응답을반환한다() throws Exception {
        given(worldStatisticsDetailQueryService
                .getWorldStatisticsDetail("luna"))
                .willReturn(new GetWorldStatisticsDetailResult(
                        new GetWorldStatisticsDetailResult.WorldResult(
                                "luna",
                                "루나",
                                CanonicalWorld.Status.ACTIVE,
                                10
                        ),
                        StatisticsDataAvailability.AVAILABLE,
                        new GetWorldStatisticsDetailResult.LatestResult(
                                SNAPSHOT_DATE,
                                320L,
                                new BigDecimal("12.40"),
                                new BigDecimal("187.3")
                        ),
                        new GetWorldStatisticsDetailResult.ComparisonResult(
                                PREVIOUS_SNAPSHOT_DATE,
                                305L,
                                15L,
                                new BigDecimal("4.92"),
                                new BigDecimal("11.65"),
                                new BigDecimal("6.44"),
                                new BigDecimal("0.75"),
                                StatisticsTrend.UP
                        ),
                        new GetWorldStatisticsDetailResult.SourceMetaResult(
                                "NEXON_OPEN_API",
                                COLLECTED_AT,
                                2581,
                                13,
                                100,
                                false
                        )
                ));

        mockMvc.perform(get("/api/v1/statistics/worlds/luna"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(
                        jsonPath("$.code")
                                .value("WORLD_STATISTICS_DETAIL_SEARCH_SUCCESS")
                )
                .andExpect(jsonPath("$.data.world.worldSlug").value("luna"))
                .andExpect(jsonPath("$.data.world.status").value("ACTIVE"))
                .andExpect(
                        jsonPath("$.data.dataAvailability")
                                .value("AVAILABLE")
                )
                .andExpect(jsonPath("$.data.latest.count").value(320))
                .andExpect(
                        jsonPath("$.data.latest.percentage").value(12.40)
                )
                .andExpect(
                        jsonPath("$.data.comparison.percentagePointChange")
                                .value(0.75)
                )
                .andExpect(jsonPath("$.data.comparison.trend").value("UP"))
                .andExpect(
                        jsonPath("$.data.sourceMeta.sampleSize").value(2581)
                );
    }

    @Test
    void 월드상세Snapshot이없으면200과NOT_COLLECTED를반환한다() throws Exception {
        given(worldStatisticsDetailQueryService
                .getWorldStatisticsDetail("luna"))
                .willReturn(GetWorldStatisticsDetailResult.notCollected(
                        new CanonicalWorld(
                                "luna",
                                "루나",
                                CanonicalWorld.Status.ACTIVE,
                                10
                        )
                ));

        mockMvc.perform(get("/api/v1/statistics/worlds/luna"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(
                        jsonPath("$.data.dataAvailability")
                                .value("NOT_COLLECTED")
                )
                .andExpect(jsonPath("$.data.latest").doesNotExist())
                .andExpect(jsonPath("$.data.comparison").doesNotExist());
    }

    @Test
    void 존재하지않는월드Slug면404를반환한다() throws Exception {
        given(worldStatisticsDetailQueryService
                .getWorldStatisticsDetail("unknown"))
                .willThrow(new WorldStatisticsException(
                        WorldStatisticsFailure.WORLD_NOT_FOUND
                ));

        mockMvc.perform(get("/api/v1/statistics/worlds/unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("STATISTICS_007"));
    }

    @Test
    void 월드History응답을반환한다() throws Exception {
        given(worldStatisticsDetailQueryService.getWorldStatisticsHistory(
                "luna",
                "7D",
                null,
                null
        )).willReturn(new GetWorldStatisticsHistoryResult(
                new GetWorldStatisticsHistoryResult.WorldResult(
                        "luna",
                        "루나"
                ),
                StatisticsDataAvailability.AVAILABLE,
                new GetWorldStatisticsHistoryResult.RangeResult(
                        "7D",
                        SNAPSHOT_DATE.minusDays(6),
                        SNAPSHOT_DATE,
                        PREVIOUS_SNAPSHOT_DATE,
                        SNAPSHOT_DATE,
                        2,
                        5
                ),
                new GetWorldStatisticsHistoryResult.RangeComparisonResult(
                        PREVIOUS_SNAPSHOT_DATE,
                        SNAPSHOT_DATE,
                        305L,
                        320L,
                        15L,
                        new BigDecimal("4.92"),
                        new BigDecimal("11.65"),
                        new BigDecimal("12.40"),
                        new BigDecimal("6.44"),
                        new BigDecimal("0.75")
                ),
                List.of(
                        new GetWorldStatisticsHistoryResult.PointResult(
                                PREVIOUS_SNAPSHOT_DATE,
                                305L,
                                new BigDecimal("11.65"),
                                new BigDecimal("186.9"),
                                2618,
                                13,
                                100,
                                false,
                                COLLECTED_AT
                        ),
                        new GetWorldStatisticsHistoryResult.PointResult(
                                SNAPSHOT_DATE,
                                320L,
                                new BigDecimal("12.40"),
                                new BigDecimal("187.3"),
                                2581,
                                13,
                                100,
                                false,
                                COLLECTED_AT
                        )
                ),
                List.of("전체 이용자 모집단이 아니라 수집한 종합 랭킹 표본입니다.")
        ));

        mockMvc.perform(get("/api/v1/statistics/worlds/luna/history")
                        .param("range", "7D"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(
                        jsonPath("$.code")
                                .value("WORLD_STATISTICS_HISTORY_SEARCH_SUCCESS")
                )
                .andExpect(jsonPath("$.data.range.preset").value("7D"))
                .andExpect(
                        jsonPath("$.data.range.requestedFrom")
                                .value("2026-07-18")
                )
                .andExpect(jsonPath("$.data.range.pointCount").value(2))
                .andExpect(jsonPath("$.data.range.missingDateCount").value(5))
                .andExpect(jsonPath("$.data.points.length()").value(2))
                .andExpect(
                        jsonPath("$.data.points[0].asOf")
                                .value("2026-07-21")
                )
                .andExpect(
                        jsonPath("$.data.rangeComparison.percentagePointChange")
                                .value(0.75)
                )
                .andExpect(jsonPath("$.data.limitations.length()").value(1));
    }

    @Test
    void 잘못된월드History기간요청이면400을반환한다() throws Exception {
        given(worldStatisticsDetailQueryService.getWorldStatisticsHistory(
                "luna",
                "5D",
                null,
                null
        )).willThrow(new WorldStatisticsException(
                WorldStatisticsFailure.INVALID_HISTORY_REQUEST
        ));

        mockMvc.perform(get("/api/v1/statistics/worlds/luna/history")
                        .param("range", "5D"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("STATISTICS_008"));
    }

    @Test
    void 월드History집계가정합하지않으면500을반환한다() throws Exception {
        given(worldStatisticsDetailQueryService.getWorldStatisticsHistory(
                "luna",
                null,
                null,
                null
        )).willThrow(new WorldStatisticsException(
                WorldStatisticsFailure.DATA_INVALID
        ));

        mockMvc.perform(get("/api/v1/statistics/worlds/luna/history"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("STATISTICS_004"));
    }
}
