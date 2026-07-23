package com.maplemetric.ranking.application.port.out;

import com.maplemetric.ranking.application.result.GetDojangRankingResult;
import com.maplemetric.ranking.application.result.GetOverallRankingResult;
import com.maplemetric.ranking.application.result.GetUnionRankingResult;
import java.time.LocalDate;

public interface LoadRankingListPort {

    GetOverallRankingResult loadOverallRanking(
            LocalDate date,
            String worldName,
            Integer worldType,
            String className,
            int page
    );

    GetUnionRankingResult loadUnionRanking(
            LocalDate date,
            String worldName,
            int page
    );

    GetDojangRankingResult loadDojangRanking(
            LocalDate date,
            String worldName,
            int difficulty,
            String className,
            int page
    );
}
