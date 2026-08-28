package com.maplemetric.ranking.infrastructure.client.nexon;

import com.maplemetric.ranking.infrastructure.client.nexon.response.DojangRankingResponse;
import com.maplemetric.ranking.infrastructure.client.nexon.response.OverallRankingResponse;
import com.maplemetric.ranking.infrastructure.client.nexon.response.UnionRankingResponse;
import com.maplemetric.common.nexon.NexonRequestClass;
import java.time.LocalDate;

public interface RankingClient {

    OverallRankingResponse getOverallRanking(
            LocalDate date,
            String worldName,
            Integer worldType,
            String className,
            int page,
            NexonRequestClass requestClass
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
