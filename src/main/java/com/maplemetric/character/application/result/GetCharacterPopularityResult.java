package com.maplemetric.character.application.result;

import com.maplemetric.character.application.port.out.LoadCharacterPopularityPort.CharacterPopularity;

public record GetCharacterPopularityResult(
        String date,
        Long popularity
) {

    public static GetCharacterPopularityResult from(
            CharacterPopularity popularity
    ) {
        return new GetCharacterPopularityResult(
                popularity.date(),
                popularity.popularity()
        );
    }
}
