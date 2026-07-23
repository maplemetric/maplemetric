package com.maplemetric.ranking.application.result;

import java.time.LocalDate;
import java.util.List;

public record GetDojangRankingResult(
        List<Ranking> ranking,
        int page,
        LocalDate asOf,
        String source
) {

    public static GetDojangRankingResult of(
            List<Ranking> ranking,
            int page,
            LocalDate asOf,
            String source
    ) {
        return new GetDojangRankingResult(
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
            Integer dojangFloor,
            Integer dojangTimeRecord
    ) {
    }
}
