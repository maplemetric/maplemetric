package com.maplemetric.ranking.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;

public record OverallRankingResponse(
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
            Long characterExp,
            Integer characterPopularity,
            @JsonProperty("character_guildname")
            String characterGuildName
    ) {
    }
}
