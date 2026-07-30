package com.maplemetric.character.presentation.dto;

import com.maplemetric.character.application.result.GetCharacterSummaryResult;

public record GetCharacterSummaryResponse(
        GetCharacterBasicResponse basic,
        GetCharacterStatResponse stat,
        GetCharacterRankingResponse ranking,
        GetCharacterUnionResponse union,
        GetCharacterSymbolResponse symbols,
        GetCharacterSkillsResponse skills,
        GetCharacterHexaResponse hexa,
        GetCharacterEquipmentResponse equipment,
        GetCharacterSetEffectResponse setEffect,
        GetCharacterPopularityResponse popularity,
        GetCharacterHyperStatResponse hyperStat,
        GetCharacterAbilityResponse ability,
        GetCharacterDojangResponse dojang,
        String dataUpdatedAt
) {

    public static GetCharacterSummaryResponse from(
            GetCharacterSummaryResult result
    ) {
        return new GetCharacterSummaryResponse(
                GetCharacterBasicResponse.from(result.basic()),
                GetCharacterStatResponse.from(result.stat()),
                GetCharacterRankingResponse.from(result.ranking()),
                GetCharacterUnionResponse.from(result.union()),
                GetCharacterSymbolResponse.from(result.symbols()),
                GetCharacterSkillsResponse.from(result.skills()),
                GetCharacterHexaResponse.from(result.hexa()),
                GetCharacterEquipmentResponse.from(result.equipment()),
                GetCharacterSetEffectResponse.from(result.setEffect()),
                GetCharacterPopularityResponse.from(result.popularity()),
                GetCharacterHyperStatResponse.from(result.hyperStat()),
                GetCharacterAbilityResponse.from(result.ability()),
                GetCharacterDojangResponse.from(result.dojang()),
                result.dataUpdatedAt()
        );
    }
}
