package com.maplemetric.character.application.result;

public record GetCharacterResult(
        String characterName
) {

    public static GetCharacterResult from(String characterName) {
        return new GetCharacterResult(characterName);
    }
}