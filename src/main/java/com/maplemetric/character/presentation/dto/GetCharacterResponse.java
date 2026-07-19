package com.maplemetric.character.presentation.dto;

import com.maplemetric.character.application.result.GetCharacterResult;

public record GetCharacterResponse(
        String characterName,
        String ocid
) {

    public static GetCharacterResponse from(GetCharacterResult result) {
        return new GetCharacterResponse(result.characterName(), result.ocid());
    }
}