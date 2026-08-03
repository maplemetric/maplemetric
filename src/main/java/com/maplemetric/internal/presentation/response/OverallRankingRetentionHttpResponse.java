package com.maplemetric.internal.presentation.response;

import com.maplemetric.ranking.api.OverallRankingRetentionPlan;
import java.time.LocalDate;
import java.util.List;

public record OverallRankingRetentionHttpResponse(
        List<LocalDate> snapshotDates,
        long collectionCount,
        long snapshotCount,
        LocalDate retainedLatestDate
) {

    public static OverallRankingRetentionHttpResponse from(
            OverallRankingRetentionPlan plan
    ) {
        return new OverallRankingRetentionHttpResponse(
                plan.snapshotDates(),
                plan.collectionCount(),
                plan.snapshotCount(),
                plan.retainedLatestDate()
        );
    }
}
