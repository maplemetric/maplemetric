package com.maplemetric.character.application.result;

public record GetCharacterSummaryResult(
        GetCharacterBasicResult basic,
        GetCharacterStatResult stat,
        GetCharacterRankingResult ranking,
        GetCharacterUnionResult union,
        GetCharacterSymbolResult symbols,
        GetCharacterSkillsResult skills,
        GetCharacterHexaResult hexa,
        GetCharacterEquipmentResult equipment,
        GetCharacterPopularityResult popularity,
        GetCharacterHyperStatResult hyperStat,
        GetCharacterAbilityResult ability,
        GetCharacterDojangResult dojang,
        String dataUpdatedAt
) {

    public static GetCharacterSummaryResult of(
            GetCharacterBasicResult basic,
            GetCharacterStatResult stat,
            GetCharacterRankingResult ranking,
            GetCharacterUnionResult union,
            GetCharacterSymbolResult symbols,
            GetCharacterSkillsResult skills,
            GetCharacterHexaResult hexa,
            GetCharacterEquipmentResult equipment,
            GetCharacterPopularityResult popularity,
            GetCharacterHyperStatResult hyperStat,
            GetCharacterAbilityResult ability,
            GetCharacterDojangResult dojang,
            String dataUpdatedAt
    ) {
        return new GetCharacterSummaryResult(
                basic,
                stat,
                ranking,
                union,
                symbols,
                skills,
                hexa,
                equipment,
                popularity,
                hyperStat,
                ability,
                dojang,
                dataUpdatedAt
        );
    }
}
