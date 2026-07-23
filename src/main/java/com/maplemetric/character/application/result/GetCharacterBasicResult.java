package com.maplemetric.character.application.result;

import com.maplemetric.character.application.port.out.LoadCharacterBasicPort.CharacterBasic;

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

    public static GetCharacterBasicResult from(CharacterBasic basic) {
        return new GetCharacterBasicResult(
                basic.characterName(),
                basic.worldName(),
                basic.characterGender(),
                basic.characterClass(),
                basic.characterClassLevel(),
                basic.characterLevel(),
                basic.characterExp(),
                basic.characterExpRate(),
                basic.characterGuildName(),
                basic.characterImage(),
                basic.characterDateCreate(),
                basic.accessFlag(),
                basic.liberationQuestClearFlag()
        );
    }
}
