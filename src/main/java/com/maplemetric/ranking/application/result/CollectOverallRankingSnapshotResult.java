package com.maplemetric.ranking.application.result;

import java.time.LocalDate;

public record CollectOverallRankingSnapshotResult(
        boolean collected,
        LocalDate asOf,
        int pageCount,
        int sampleSize
) {

    public static CollectOverallRankingSnapshotResult collected(
            LocalDate asOf,
            int pageCount,
            int sampleSize
    ) {
        return new CollectOverallRankingSnapshotResult(
                true,
                asOf,
                pageCount,
                sampleSize
        );
    }

    public static CollectOverallRankingSnapshotResult skipped(
            LocalDate asOf
    ) {
        return new CollectOverallRankingSnapshotResult(
                false,
                asOf,
                0,
                0
        );
    }
}
