package com.maplemetric.ranking.api;

import java.time.LocalDate;

public record CollectOverallRankingSnapshotRequest(
        LocalDate rankingDate,
        int maxPages
) {
}
