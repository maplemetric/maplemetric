package com.maplemetric.character.application.result;

import com.maplemetric.character.infrastructure.client.dto.CharacterDojangResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterRankingResponse;
import java.util.Objects;

public record GetCharacterRankingResult(
        Integer overallRank,
        Integer worldRank,
        Integer classRank,
        Integer worldClassRank,
        Integer dojangFloor
) {

    public static GetCharacterRankingResult of(
            String characterName,
            CharacterRankingResponse overallRankingResponse,
            CharacterRankingResponse worldRankingResponse,
            CharacterRankingResponse classRankingResponse,
            CharacterRankingResponse worldClassRankingResponse,
            CharacterDojangResponse dojangResponse
    ) {
        return new GetCharacterRankingResult(
                extractRank(characterName, overallRankingResponse),
                extractRank(characterName, worldRankingResponse),
                extractRank(characterName, classRankingResponse),
                extractRank(characterName, worldClassRankingResponse),
                dojangResponse == null ? null : dojangResponse.dojangBestFloor()
        );
    }

    private static Integer extractRank(
            String characterName,
            CharacterRankingResponse response
    ) {
        if (response == null
                || response.ranking() == null) {
            return null;
        }

        return response.ranking()
                .stream()
                .filter(ranking -> ranking != null)
                .filter(ranking -> Objects.equals(
                        characterName,
                        ranking.characterName()
                ))
                .findFirst()
                .map(ranking -> ranking.ranking())
                .orElse(null);
    }
}