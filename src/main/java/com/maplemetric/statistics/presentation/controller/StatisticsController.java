package com.maplemetric.statistics.presentation.controller;

import com.maplemetric.common.ApiResponse;
import com.maplemetric.statistics.application.result.GetJobStatisticsDetailResult;
import com.maplemetric.statistics.application.result.GetJobStatisticsHistoryResult;
import com.maplemetric.statistics.application.result.GetJobStatisticsResult;
import com.maplemetric.statistics.application.result.GetWorldStatisticsResult;
import com.maplemetric.statistics.application.service.JobStatisticsDetailQueryService;
import com.maplemetric.statistics.application.service.StatisticsQueryService;
import com.maplemetric.statistics.presentation.code.StatisticsSuccessCode;
import com.maplemetric.statistics.presentation.response.GetJobStatisticsDetailResponse;
import com.maplemetric.statistics.presentation.response.GetJobStatisticsHistoryResponse;
import com.maplemetric.statistics.presentation.response.GetJobStatisticsResponse;
import com.maplemetric.statistics.presentation.response.GetWorldStatisticsResponse;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/statistics")
public class StatisticsController {

    private final StatisticsQueryService statisticsQueryService;
    private final JobStatisticsDetailQueryService
            jobStatisticsDetailQueryService;

    public StatisticsController(
            StatisticsQueryService statisticsQueryService,
            JobStatisticsDetailQueryService
                    jobStatisticsDetailQueryService
    ) {
        this.statisticsQueryService = statisticsQueryService;
        this.jobStatisticsDetailQueryService =
                jobStatisticsDetailQueryService;
    }

    @GetMapping("/jobs")
    public ApiResponse<GetJobStatisticsResponse> getJobStatistics() {
        GetJobStatisticsResult result =
                statisticsQueryService.getJobStatistics();

        return ApiResponse.ok(
                StatisticsSuccessCode.JOB_STATISTICS_SEARCH_SUCCESS,
                GetJobStatisticsResponse.from(result)
        );
    }

    @GetMapping("/worlds")
    public ApiResponse<GetWorldStatisticsResponse> getWorldStatistics() {
        GetWorldStatisticsResult result =
                statisticsQueryService.getWorldStatistics();

        return ApiResponse.ok(
                StatisticsSuccessCode.WORLD_STATISTICS_SEARCH_SUCCESS,
                GetWorldStatisticsResponse.from(result)
        );
    }

    @GetMapping("/jobs/{jobSlug}")
    public ApiResponse<GetJobStatisticsDetailResponse>
            getJobStatisticsDetail(
                    @PathVariable String jobSlug
            ) {
        GetJobStatisticsDetailResult result =
                jobStatisticsDetailQueryService
                        .getJobStatisticsDetail(jobSlug);

        return ApiResponse.ok(
                StatisticsSuccessCode
                        .JOB_STATISTICS_DETAIL_SEARCH_SUCCESS,
                GetJobStatisticsDetailResponse.from(result)
        );
    }

    @GetMapping("/jobs/{jobSlug}/history")
    public ApiResponse<GetJobStatisticsHistoryResponse>
            getJobStatisticsHistory(
                    @PathVariable String jobSlug,
                    @RequestParam(required = false)
                    String range,
                    @RequestParam(required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate from,
                    @RequestParam(required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate to
            ) {
        GetJobStatisticsHistoryResult result =
                jobStatisticsDetailQueryService
                        .getJobStatisticsHistory(
                                jobSlug,
                                range,
                                from,
                                to
                        );

        return ApiResponse.ok(
                StatisticsSuccessCode
                        .JOB_STATISTICS_HISTORY_SEARCH_SUCCESS,
                GetJobStatisticsHistoryResponse.from(result)
        );
    }
}
