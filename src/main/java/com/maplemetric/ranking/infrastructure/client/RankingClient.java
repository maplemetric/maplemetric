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
