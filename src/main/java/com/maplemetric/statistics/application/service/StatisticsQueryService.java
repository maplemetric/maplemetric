package com.maplemetric.statistics.application.service;

import com.maplemetric.ranking.api.OverallRankingStatisticsQuery;
import com.maplemetric.ranking.api.OverallRankingStatisticsSnapshot;
import com.maplemetric.ranking.api.OverallRankingWorldStatisticsQuery;
import com.maplemetric.ranking.api.OverallRankingWorldStatisticsSnapshot;
import com.maplemetric.statistics.application.result.GetJobStatisticsResult;
import com.maplemetric.statistics.application.result.GetWorldStatisticsResult;
import org.springframework.stereotype.Service;

@Service
public class StatisticsQueryService {

    private final OverallRankingStatisticsQuery overallRankingStatisticsQuery;
    private final OverallRankingWorldStatisticsQuery overallRankingWorldStatisticsQuery;

    public StatisticsQueryService(
            OverallRankingStatisticsQuery overallRankingStatisticsQuery,
            OverallRankingWorldStatisticsQuery overallRankingWorldStatisticsQuery
    ) {
        this.overallRankingStatisticsQuery = overallRankingStatisticsQuery;
        this.overallRankingWorldStatisticsQuery = overallRankingWorldStatisticsQuery;
    }

    public GetJobStatisticsResult getJobStatistics() {
        OverallRankingStatisticsSnapshot snapshot =
                overallRankingStatisticsQuery.getLatestJobStatistics();

        return GetJobStatisticsResult.from(snapshot);
    }

    public GetWorldStatisticsResult getWorldStatistics() {
        OverallRankingWorldStatisticsSnapshot snapshot =
                overallRankingWorldStatisticsQuery.getLatestWorldStatistics();

        return GetWorldStatisticsResult.from(snapshot);
    }
}
