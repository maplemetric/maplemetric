package com.maplemetric.character.presentation.dto;

import com.maplemetric.character.application.result.GetCharacterRankingResult;

public record GetCharacterRankingResponse(
        Integer overallRank,
        Integer worldRank,
        Integer classRank,
        Integer worldClassRank,
        Integer dojangFloor
) {

    public static GetCharacterRankingResponse from(
            GetCharacterRankingResult result
    ) {
        return new GetCharacterRankingResponse(
                result.overallRank(),
                result.worldRank(),
                result.classRank(),
                result.worldClassRank(),
                result.dojangFloor()
        );
    }
}