package com.maplemetric.internal.presentation.response;

import com.maplemetric.ranking.api.CollectOverallRankingSnapshotOutcome;
import com.maplemetric.ranking.api.OverallRankingCollectionStatus;
import java.time.LocalDate;

public record CollectOverallRankingSnapshotHttpResponse(
        OverallRankingCollectionStatus status,
        LocalDate asOf,
        Integer pageCount,
        Integer sampleSize,
        Boolean truncated
) {

    public static CollectOverallRankingSnapshotHttpResponse from(
            CollectOverallRankingSnapshotOutcome outcome
    ) {
        return new CollectOverallRankingSnapshotHttpResponse(
                outcome.status(),
                outcome.asOf(),
                outcome.pageCount(),
                outcome.sampleSize(),
                outcome.truncated()
        );
    }
}
