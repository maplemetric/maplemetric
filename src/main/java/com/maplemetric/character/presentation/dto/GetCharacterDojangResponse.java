package com.maplemetric.character.presentation.dto;

import com.maplemetric.character.application.result.GetCharacterDojangResult;

public record GetCharacterDojangResponse(
        String date,
        String characterClass,
        String worldName,
        Integer bestFloor,
        String recordDate,
        Integer bestTime
) {

    public static GetCharacterDojangResponse from(
            GetCharacterDojangResult result
    ) {
        return new GetCharacterDojangResponse(
                result.date(),
                result.characterClass(),
                result.worldName(),
                result.bestFloor(),
                result.recordDate(),
                result.bestTime()
        );
    }
}
