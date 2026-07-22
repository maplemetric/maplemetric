package com.maplemetric.ranking.application.result;

import com.maplemetric.ranking.infrastructure.client.dto.UnionRankingResponse;
import java.time.LocalDate;
import java.util.List;

public record GetUnionRankingResult(
        List<Ranking> ranking,
        int page,
        LocalDate asOf,
        String source
) {

    public static GetUnionRankingResult from(
            UnionRankingResponse response,
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
                        item.unionLevel(),
                        item.unionPower()
                ))
                .toList();

        return new GetUnionRankingResult(
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
            Integer unionLevel,
            Long unionPower
    ) {
    }
}
