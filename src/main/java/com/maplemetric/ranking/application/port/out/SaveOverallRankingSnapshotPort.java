package com.maplemetric.ranking.application.port.out;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public interface SaveOverallRankingSnapshotPort {

    boolean existsOverallRankingCollection(
            LocalDate snapshotDate,
            String worldName,
            Integer worldType,
            String className
    );

    void saveOverallRankingSnapshot(
            OverallRankingCollection collection
    );

    record OverallRankingCollection(
            LocalDate snapshotDate,
            String worldName,
            Integer worldType,
            String className,
            String source,
            int pageCount,
            int requestedMaxPages,
            boolean truncated,
            Instant collectedAt,
            List<RankingRow> rows
    ) {
    }

    record RankingRow(
            Integer ranking,
            String characterName,
            String worldName,
            String className,
            String subClassName,
            Integer characterLevel,
            Long characterExp,
            Integer characterPopularity,
            String characterGuildName
    ) {
    }
}
