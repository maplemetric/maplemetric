package com.maplemetric.ranking.application.result;

import com.maplemetric.ranking.infrastructure.client.nexon.response.OverallRankingResponse;
import java.time.LocalDate;
import java.util.List;

public record GetOverallRankingResult(
        List<Ranking> ranking,
        int page,
        LocalDate asOf,
        String source
) {

    public static GetOverallRankingResult from(
            OverallRankingResponse response,
            int page,
            LocalDate requestedDate
    ) {
        LocalDate asOf = RankingResultSupport.resolveAsOf(
                response.ranking(),
                requestedDate,
                item -> item.date()
        );

        List<Ranking> ranking = response.ranking()
                .stream()
                .map(item -> new Ranking(
                        item.ranking(),
                        item.characterName(),
                        item.worldName(),
                        item.className(),
                        item.subClassName(),
                        item.characterLevel(),
                        item.characterExp(),
                        item.characterPopularity(),
                        item.characterGuildName()
                ))
                .toList();

        return new GetOverallRankingResult(
                ranking,
                page,
                asOf,
                RankingResultSupport.SOURCE
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
