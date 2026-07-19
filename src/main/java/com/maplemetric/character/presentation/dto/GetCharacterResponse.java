package com.maplemetric.character.presentation.dto;

import com.maplemetric.character.application.result.GetCharacterResult;

public record GetCharacterResponse(
        String characterName
) {

    public static GetCharacterResponse from(GetCharacterResult result) {
        return new GetCharacterResponse(result.characterName());
    }
}