package com.maplemetric.character.application.result;

public record GetCharacterResult(
        String characterName,
        String ocid
) {

    public static GetCharacterResult of(String characterName, String ocid) {
        return new GetCharacterResult(characterName, ocid);
    }
}