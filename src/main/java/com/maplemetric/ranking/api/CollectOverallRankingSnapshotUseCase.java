package com.maplemetric.ranking.api;

public interface CollectOverallRankingSnapshotUseCase {

    CollectOverallRankingSnapshotOutcome collect(
            CollectOverallRankingSnapshotRequest request
    );
}
