package com.maplemetric.character.application.result;

import com.maplemetric.character.infrastructure.client.dto.CharacterDojangResponse;
import com.maplemetric.ranking.api.CharacterRanking;

public record GetCharacterRankingResult(
        Integer overallRank,
        Integer worldRank,
        Integer classRank,
        Integer worldClassRank,
        Integer dojangFloor
) {

    public static GetCharacterRankingResult of(
            CharacterRanking ranking,
            CharacterDojangResponse dojangResponse
    ) {
        return new GetCharacterRankingResult(
                ranking == null ? null : ranking.overallRank(),
                ranking == null ? null : ranking.worldRank(),
                ranking == null ? null : ranking.classRank(),
                ranking == null ? null : ranking.worldClassRank(),
                dojangResponse == null ? null : dojangResponse.dojangBestFloor()
        );
    }
}
