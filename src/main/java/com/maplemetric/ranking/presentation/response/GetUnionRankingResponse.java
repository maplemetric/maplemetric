package com.maplemetric.ranking.presentation.response;

import com.maplemetric.ranking.application.result.GetUnionRankingResult;
import java.time.LocalDate;
import java.util.List;

public record GetUnionRankingResponse(
        List<Ranking> ranking,
        int page,
        LocalDate asOf,
        String source
) {

    public static GetUnionRankingResponse from(
            GetUnionRankingResult result
    ) {
        List<Ranking> ranking = result.ranking()
                .stream()
                .map(item -> new Ranking(
                        item.ranking(),
                        item.characterName(),
                        item.worldName(),
                        item.className(),
                        item.subClassName(),
                        item.unionLevel(),
                        item.unionPower()
                ))
                .toList();

        return new GetUnionRankingResponse(
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
            Integer unionLevel,
            Long unionPower
    ) {
    }
}
