package com.maplemetric.ranking.infrastructure.client.nexon.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;

public record DojangRankingResponse(
        List<Ranking> ranking
) {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Ranking(
            String date,
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
