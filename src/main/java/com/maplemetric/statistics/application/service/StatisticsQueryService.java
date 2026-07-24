package com.maplemetric.statistics.application.service;

import com.maplemetric.ranking.api.OverallRankingStatisticsQuery;
import com.maplemetric.ranking.api.OverallRankingStatisticsQueryException;
import com.maplemetric.ranking.api.OverallRankingStatisticsSnapshot;
import com.maplemetric.statistics.application.result.GetJobStatisticsResult;
import com.maplemetric.statistics.domain.exception.StatisticsException;
import com.maplemetric.statistics.presentation.code.StatisticsErrorCode;
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
        try {
            OverallRankingStatisticsSnapshot snapshot =
                    overallRankingStatisticsQuery.getLatestJobStatistics();

            return GetJobStatisticsResult.from(snapshot);
        } catch (OverallRankingStatisticsQueryException exception) {
            throw new StatisticsException(
                    resolveStatisticsErrorCode(exception)
            );
        }
    }

    private StatisticsErrorCode resolveStatisticsErrorCode(
            OverallRankingStatisticsQueryException exception
    ) {
        return switch (exception.getFailure()) {
            case NOT_FOUND ->
                    StatisticsErrorCode.JOB_STATISTICS_SNAPSHOT_NOT_FOUND;
            case DATA_INVALID ->
                    StatisticsErrorCode.JOB_STATISTICS_DATA_INVALID;
        };
    }
}
