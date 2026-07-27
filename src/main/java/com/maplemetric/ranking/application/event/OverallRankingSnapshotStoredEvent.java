package com.maplemetric.ranking.application.event;

import java.time.LocalDate;
import java.util.List;

public record OverallRankingSnapshotStoredEvent(
        LocalDate snapshotDate,
        List<ObservedName> jobNames,
        List<ObservedName> worldNames
) {

    public OverallRankingSnapshotStoredEvent {
        jobNames = List.copyOf(jobNames);
        worldNames = List.copyOf(worldNames);
    }

    public record ObservedName(
            String name,
            long rowCount
    ) {
    }
}
