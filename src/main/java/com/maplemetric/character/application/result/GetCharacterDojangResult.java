package com.maplemetric.character.application.result;

import com.maplemetric.character.infrastructure.client.dto.CharacterDojangResponse;

public record GetCharacterDojangResult(
        String date,
        String characterClass,
        String worldName,
        Integer bestFloor,
        String recordDate,
        Integer bestTime
) {

    public static GetCharacterDojangResult from(
            CharacterDojangResponse response
    ) {
        return new GetCharacterDojangResult(
                response.date(),
                response.characterClass(),
                response.worldName(),
                response.dojangBestFloor(),
                response.dateDojangRecord(),
                response.dojangBestTime()
        );
    }
}
