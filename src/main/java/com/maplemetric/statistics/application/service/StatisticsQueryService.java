package com.maplemetric.statistics.application.service;

import com.maplemetric.ranking.api.OverallRankingStatisticsQuery;
import com.maplemetric.ranking.api.OverallRankingStatisticsSnapshot;
import com.maplemetric.statistics.application.result.GetJobStatisticsResult;
import org.springframework.stereotype.Service;

@Service
public class StatisticsQueryService {

    private final OverallRankingStatisticsQuery overallRankingStatisticsQuery;

    public StatisticsQueryService(
            OverallRankingStatisticsQuery overallRankingStatisticsQuery
    ) {
        this.overallRankingStatisticsQuery = overallRankingStatisticsQuery;
    }

    public GetJobStatisticsResult getJobStatistics() {
        OverallRankingStatisticsSnapshot snapshot =
                overallRankingStatisticsQuery.getLatestJobStatistics();

        return GetJobStatisticsResult.from(snapshot);
    }
}
