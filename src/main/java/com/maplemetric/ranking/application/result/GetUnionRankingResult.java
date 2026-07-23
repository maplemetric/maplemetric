package com.maplemetric.ranking.application.result;

import java.time.LocalDate;
import java.util.List;

public record GetUnionRankingResult(
        List<Ranking> ranking,
        int page,
        LocalDate asOf,
        String source
) {

    public static GetUnionRankingResult of(
            List<Ranking> ranking,
            int page,
            LocalDate asOf,
            String source
    ) {
        return new GetUnionRankingResult(
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
            Integer unionLevel,
            Long unionPower
    ) {
    }
}
