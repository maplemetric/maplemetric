package com.maplemetric.ranking.application.result;

import java.time.LocalDate;
import java.util.List;

public record GetOverallRankingResult(
        List<Ranking> ranking,
        int page,
        LocalDate asOf,
        String source
) {

    public static GetOverallRankingResult of(
            List<Ranking> ranking,
            int page,
            LocalDate asOf,
            String source
    ) {
        return new GetOverallRankingResult(
                ranking,
                page,
                asOf,
                source
        );
    }

    public record Ranking(
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
