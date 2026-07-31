package com.maplemetric.ranking.api;

import java.time.LocalDate;
import java.util.List;

public interface OverallRankingWorldStatisticsHistoryQuery {

    List<OverallRankingWorldStatisticsSnapshot> getWorldStatisticsHistory(
            LocalDate from,
            LocalDate to
    );
}
