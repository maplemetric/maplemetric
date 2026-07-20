package com.maplemetric.character.presentation.dto;

import com.maplemetric.character.application.result.GetCharacterBasicResult;

public record GetCharacterBasicResponse(
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

    public static GetCharacterBasicResponse from(GetCharacterBasicResult result) {
        return new GetCharacterBasicResponse(
                result.characterName(),
                result.worldName(),
                result.characterGender(),
                result.characterClass(),
                result.characterClassLevel(),
                result.characterLevel(),
                result.characterExp(),
                result.characterExpRate(),
                result.characterGuildName(),
                result.characterImage(),
                result.characterDateCreate(),
                result.accessFlag(),
                result.liberationQuestClearFlag()
        );
    }
}
