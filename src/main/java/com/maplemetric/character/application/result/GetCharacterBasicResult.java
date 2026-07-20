package com.maplemetric.character.application.result;

import com.maplemetric.character.infrastructure.client.dto.CharacterBasicResponse;

public record GetCharacterBasicResult(
        String characterName,
        String worldName,
        String characterGender,
        String characterClass,
        String characterClassLevel,
        Integer characterLevel,
        Long characterExp,
        String characterExpRate,
        String characterGuildName,
        String characterImage,
        String characterDateCreate,
        String accessFlag,
        String liberationQuestClearFlag
) {

    public static GetCharacterBasicResult from(CharacterBasicResponse response) {
        return new GetCharacterBasicResult(
                response.characterName(),
                response.worldName(),
                response.characterGender(),
                response.characterClass(),
                response.characterClassLevel(),
                response.characterLevel(),
                response.characterExp(),
                response.characterExpRate(),
                response.characterGuildName(),
                response.characterImage(),
                response.characterDateCreate(),
                response.accessFlag(),
                response.liberationQuestClearFlag()
        );
    }
}
