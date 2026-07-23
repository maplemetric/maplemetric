package com.maplemetric.ranking.presentation.response;

import com.maplemetric.ranking.application.result.GetDojangRankingResult;
import java.time.LocalDate;
import java.util.List;

public record GetDojangRankingResponse(
        List<Ranking> ranking,
        int page,
        LocalDate asOf,
        String source
) {

    public static GetDojangRankingResponse from(
            GetDojangRankingResult result
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
                        item.dojangFloor(),
                        item.dojangTimeRecord()
                ))
                .toList();

        return new GetDojangRankingResponse(
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
            Integer dojangFloor,
            Integer dojangTimeRecord
    ) {
    }
}
