package com.maplemetric.character.application.port.out;

public interface LoadCharacterBasicPort {

    String resolveOcid(String characterName);

    CharacterBasic loadCharacterBasic(String ocid);

    record CharacterBasic(
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
    }
}
