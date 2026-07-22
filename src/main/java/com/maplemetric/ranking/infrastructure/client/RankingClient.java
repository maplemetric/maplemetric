package com.maplemetric.ranking.infrastructure.client;

import com.maplemetric.ranking.infrastructure.client.dto.DojangRankingResponse;
import com.maplemetric.ranking.infrastructure.client.dto.OverallRankingResponse;
import com.maplemetric.ranking.infrastructure.client.dto.UnionRankingResponse;
import java.time.LocalDate;

public interface RankingClient {

    OverallRankingResponse getOverallRanking(
            LocalDate date,
            String worldName,
            Integer worldType,
            String className,
            int page
    );

    OverallRankingResponse getCharacterOverallRanking(
            String ocid,
            LocalDate date
    );

    OverallRankingResponse getCharacterWorldRanking(
            String ocid,
            String worldName,
            LocalDate date
    );

    OverallRankingResponse getCharacterClassRanking(
            String ocid,
            String classRankingFilter,
            LocalDate date
    );

    OverallRankingResponse getCharacterWorldClassRanking(
            String ocid,
            String worldName,
            String classRankingFilter,
            LocalDate date
    );

    UnionRankingResponse getUnionRanking(
            LocalDate date,
            String worldName,
            int page
    );

    DojangRankingResponse getDojangRanking(
            LocalDate date,
            String worldName,
            int difficulty,
            String className,
            int page
    );
}
