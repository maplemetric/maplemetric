package com.maplemetric.ranking.application.result;

import com.maplemetric.ranking.infrastructure.client.dto.DojangRankingResponse;
import java.time.LocalDate;
import java.util.List;

public record GetDojangRankingResult(
        List<Ranking> ranking,
        int page,
        LocalDate asOf,
        String source
) {

    public static GetDojangRankingResult from(
            DojangRankingResponse response,
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
                        item.dojangFloor(),
                        item.dojangTimeRecord()
                ))
                .toList();

        return new GetDojangRankingResult(
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
            Integer dojangFloor,
            Integer dojangTimeRecord
    ) {
    }
}
