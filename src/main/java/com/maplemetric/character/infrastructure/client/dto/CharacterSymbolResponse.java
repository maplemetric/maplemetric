package com.maplemetric.character.infrastructure.client.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CharacterSymbolResponse(
        String date,
        String characterClass,
        List<Symbol> symbol
) {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Symbol(
            String symbolName,
            Integer symbolLevel
    ) {
    }
}
