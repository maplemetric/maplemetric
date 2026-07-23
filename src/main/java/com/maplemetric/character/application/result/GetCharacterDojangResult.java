package com.maplemetric.character.application.result;

import com.maplemetric.character.application.port.out.LoadCharacterDojangPort.CharacterDojang;

public record GetCharacterDojangResult(
        String date,
        String characterClass,
        String worldName,
        Integer bestFloor,
        String recordDate,
        Integer bestTime
) {

    public static GetCharacterDojangResult from(
            CharacterDojang dojang
    ) {
        return new GetCharacterDojangResult(
                dojang.date(),
                dojang.characterClass(),
                dojang.worldName(),
                dojang.bestFloor(),
                dojang.recordDate(),
                dojang.bestTime()
        );
    }
}
