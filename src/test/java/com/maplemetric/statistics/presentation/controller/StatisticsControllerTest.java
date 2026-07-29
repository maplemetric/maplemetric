package com.maplemetric.statistics.presentation.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.maplemetric.ranking.api.OverallRankingComparisonQueryException;
import com.maplemetric.ranking.api.OverallRankingComparisonQueryFailure;
import com.maplemetric.ranking.api.OverallRankingWorldStatisticsQueryException;
import com.maplemetric.ranking.api.OverallRankingWorldStatisticsQueryFailure;
import com.maplemetric.statistics.application.exception.JobStatisticsException;
import com.maplemetric.statistics.application.exception.JobStatisticsFailure;
import com.maplemetric.statistics.application.result.GetJobStatisticsResult;
import com.maplemetric.statistics.application.result.GetJobStatisticsResult.JobComparisonResult;
import com.maplemetric.statistics.application.result.GetJobStatisticsResult.JobStatisticsResult;
import com.maplemetric.statistics.application.result.StatisticsTrend;
import com.maplemetric.statistics.application.result.GetWorldStatisticsResult;
import com.maplemetric.statistics.application.result.GetWorldStatisticsResult.WorldStatisticsResult;
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
                                                new BigDecimal("187.3")
                                        ),
                                        new WorldStatisticsResult(
                                                "bera",
                                                "베라",
                                                0L,
                                                new BigDecimal("0.00"),
                                                null
                                        )
                                ),
                                2581,
                                SNAPSHOT_DATE,
                                "NEXON_OPEN_API",
                                COLLECTED_AT,
                                13,
                                100,
                                false
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
                                false
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
}
