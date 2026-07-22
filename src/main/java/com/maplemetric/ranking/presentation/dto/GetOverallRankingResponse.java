package com.maplemetric.ranking.presentation.dto;

import com.maplemetric.ranking.application.result.GetOverallRankingResult;
import java.time.LocalDate;
import java.util.List;

public record GetOverallRankingResponse(
        List<Ranking> ranking,
        int page,
        LocalDate asOf,
        String source
) {

    public static GetOverallRankingResponse from(
            GetOverallRankingResult result
    ) {
        List<Ranking> ranking = result.ranking()
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

        return new GetOverallRankingResponse(
                ranking,
                result.page(),
                result.asOf(),
                result.source()
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
