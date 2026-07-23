package com.maplemetric.character.application.port.out;

public interface LoadCharacterDojangPort {

    CharacterDojang loadCharacterDojang(String ocid);

    record CharacterDojang(
            String date,
            String characterClass,
            String worldName,
            Integer bestFloor,
            String recordDate,
            Integer bestTime
    ) {
    }
}
