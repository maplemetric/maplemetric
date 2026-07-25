package com.maplemetric.ranking.api;

public interface OverallRankingComparisonQuery {

    OverallRankingStatisticsComparisonSnapshot getJobStatisticsComparison();

    OverallRankingWorldStatisticsComparisonSnapshot getWorldStatisticsComparison();
}
