package com.maplemetric.ranking.api;

import java.time.LocalDate;

public record CollectOverallRankingSnapshotOutcome(
        OverallRankingCollectionStatus status,
        LocalDate asOf,
        Integer pageCount,
        Integer sampleSize,
        Boolean truncated
) {
}
