package com.maplemetric.character.infrastructure.client.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CharacterSetEffectResponse(
        String date,
        List<SetEffect> setEffect
) {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record SetEffect(
            String setName,
            Integer totalSetCount,
            List<SetEffectInfo> setEffectInfo,
            List<SetEffectInfo> setOptionFull
    ) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record SetEffectInfo(
            Integer setCount,
            String setOption
    ) {
    }
}
