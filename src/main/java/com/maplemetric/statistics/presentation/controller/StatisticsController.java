package com.maplemetric.statistics.presentation.controller;

import com.maplemetric.common.ApiResponse;
import com.maplemetric.statistics.application.result.GetJobStatisticsResult;
import com.maplemetric.statistics.application.result.GetWorldStatisticsResult;
import com.maplemetric.statistics.application.service.StatisticsQueryService;
import com.maplemetric.statistics.presentation.code.StatisticsSuccessCode;
import com.maplemetric.statistics.presentation.response.GetJobStatisticsResponse;
import com.maplemetric.statistics.presentation.response.GetWorldStatisticsResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/statistics")
public class StatisticsController {

    private final StatisticsQueryService statisticsQueryService;

    public StatisticsController(
            StatisticsQueryService statisticsQueryService
    ) {
        this.statisticsQueryService = statisticsQueryService;
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
}
