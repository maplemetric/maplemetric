package com.maplemetric.character.infrastructure.client.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CharacterBasicResponse(
        String data,
        String characterName,
        String worldName,
        String characterGender,
        String characterClass,
        String characterClassLevel,
        Integer characterLever,
        Long characterExp,
        String characterExpRate,
        String characterGuildName,
        String characterImage,
        String characterDateCreate,
        String accessFlag,
        String liberationQuestClearFlag
) {
}
