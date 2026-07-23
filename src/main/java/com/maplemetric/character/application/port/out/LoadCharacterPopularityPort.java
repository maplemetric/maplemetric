package com.maplemetric.character.application.port.out;

public interface LoadCharacterPopularityPort {

    CharacterPopularity loadCharacterPopularity(String ocid);

    record CharacterPopularity(
            String date,
            Long popularity
    ) {
    }
}
