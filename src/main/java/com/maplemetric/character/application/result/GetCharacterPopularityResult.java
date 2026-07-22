package com.maplemetric.character.application.result;

import com.maplemetric.character.infrastructure.client.dto.CharacterPopularityResponse;

public record GetCharacterPopularityResult(
        String date,
        Long popularity
) {

    public static GetCharacterPopularityResult from(
            CharacterPopularityResponse response
    ) {
        return new GetCharacterPopularityResult(
                response.date(),
                response.popularity()
        );
    }
}
