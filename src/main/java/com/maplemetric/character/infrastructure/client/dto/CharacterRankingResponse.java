package com.maplemetric.character.infrastructure.client.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;

public record CharacterRankingResponse(
        List<Ranking> ranking
) {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Ranking(
            Integer ranking,
            String characterName,
            String worldName,
            String className,
            String subClassName
    ) {
    }
}
