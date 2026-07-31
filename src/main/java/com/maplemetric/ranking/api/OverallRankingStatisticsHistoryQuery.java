package com.maplemetric.ranking.api;

import java.time.LocalDate;
import java.util.List;

public interface OverallRankingStatisticsHistoryQuery {

    List<OverallRankingStatisticsSnapshot> getJobStatisticsHistory(
            LocalDate from,
            LocalDate to
    );
}
