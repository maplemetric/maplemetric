package com.maplemetric.statistics.application.service;

import com.maplemetric.ranking.api.OverallRankingComparisonQuery;
import com.maplemetric.ranking.api.OverallRankingComparisonQueryException;
import com.maplemetric.ranking.api.OverallRankingStatisticsComparisonSnapshot;
import com.maplemetric.ranking.api.OverallRankingWorldStatisticsQuery;
import com.maplemetric.ranking.api.OverallRankingWorldStatisticsSnapshot;
import com.maplemetric.statistics.application.exception.JobStatisticsException;
import com.maplemetric.statistics.application.exception.JobStatisticsFailure;
import com.maplemetric.statistics.application.result.GetJobStatisticsResult;
import com.maplemetric.statistics.application.result.GetWorldStatisticsResult;
import org.springframework.stereotype.Service;

@Service
public class StatisticsQueryService {

    private final OverallRankingComparisonQuery overallRankingComparisonQuery;
    private final OverallRankingWorldStatisticsQuery overallRankingWorldStatisticsQuery;

    public StatisticsQueryService(
            OverallRankingComparisonQuery overallRankingComparisonQuery,
            OverallRankingWorldStatisticsQuery overallRankingWorldStatisticsQuery
    ) {
        this.overallRankingComparisonQuery = overallRankingComparisonQuery;
        this.overallRankingWorldStatisticsQuery = overallRankingWorldStatisticsQuery;
    }

    public GetJobStatisticsResult getJobStatistics() {
        OverallRankingStatisticsComparisonSnapshot comparison =
                loadJobStatisticsComparison();

        return GetJobStatisticsResult.from(comparison);
    }

    private OverallRankingStatisticsComparisonSnapshot loadJobStatisticsComparison() {
        try {
            return overallRankingComparisonQuery.getJobStatisticsComparison();
        } catch (OverallRankingComparisonQueryException exception) {
            throw new JobStatisticsException(
                    toJobStatisticsFailure(exception),
                    exception
            );
        }
    }

    private JobStatisticsFailure toJobStatisticsFailure(
            OverallRankingComparisonQueryException exception
    ) {
        return switch (exception.getFailure()) {
            case NOT_FOUND -> JobStatisticsFailure.SNAPSHOT_NOT_FOUND;
            case DATA_INVALID -> JobStatisticsFailure.DATA_INVALID;
        };
    }

    public GetWorldStatisticsResult getWorldStatistics() {
        OverallRankingWorldStatisticsSnapshot snapshot =
                overallRankingWorldStatisticsQuery.getLatestWorldStatistics();

        return GetWorldStatisticsResult.from(snapshot);
    }
}
