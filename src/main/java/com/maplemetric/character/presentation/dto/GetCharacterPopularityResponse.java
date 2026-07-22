package com.maplemetric.character.presentation.dto;

import com.maplemetric.character.application.result.GetCharacterPopularityResult;

public record GetCharacterPopularityResponse(
        String date,
        Long popularity
) {

    public static GetCharacterPopularityResponse from(
            GetCharacterPopularityResult result
    ) {
        return new GetCharacterPopularityResponse(
                result.date(),
                result.popularity()
        );
    }
}
