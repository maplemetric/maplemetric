package com.maplemetric.ranking.api;

import java.util.List;

public interface OverallRankingStatisticsTrendQuery {

    List<OverallRankingStatisticsSnapshot> getJobStatisticsTrend(int days);
}
