package com.maplemetric.ranking.application.port.out;

import java.time.LocalDate;
import java.util.List;

public interface LoadCharacterRankingPort {

    List<RankingEntry> loadOverallRanking(
            String ocid,
            LocalDate date
    );

    List<RankingEntry> loadWorldRanking(
            String ocid,
            String worldName,
            LocalDate date
    );

    List<RankingEntry> loadClassRanking(
            String ocid,
            String classRankingFilter,
            LocalDate date
    );

    List<RankingEntry> loadWorldClassRanking(
            String ocid,
            String worldName,
            String classRankingFilter,
            LocalDate date
    );

    record RankingEntry(
            Integer ranking,
            String characterName,
            String className,
            String subClassName
    ) {
    }
}
