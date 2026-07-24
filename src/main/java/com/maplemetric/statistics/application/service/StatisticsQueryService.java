package com.maplemetric.statistics.application.service;

import com.maplemetric.ranking.api.OverallRankingStatisticsQuery;
import com.maplemetric.ranking.api.OverallRankingStatisticsSnapshot;
import com.maplemetric.statistics.application.result.GetJobStatisticsResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StatisticsQueryService {

    private final OverallRankingStatisticsQuery overallRankingStatisticsQuery;

    public StatisticsQueryService(
            OverallRankingStatisticsQuery overallRankingStatisticsQuery
    ) {
        this.overallRankingStatisticsQuery = overallRankingStatisticsQuery;
    }

    @Transactional(readOnly = true)
    public GetJobStatisticsResult getJobStatistics() {
        OverallRankingStatisticsSnapshot snapshot =
                overallRankingStatisticsQuery.getLatestJobStatistics();

        return GetJobStatisticsResult.from(snapshot);
    }
}
