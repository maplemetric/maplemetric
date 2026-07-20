package com.maplemetric.character.application.result;

public record GetCharacterSummaryResult(
        GetCharacterBasicResult basic,
        GetCharacterStatResult stat,
        GetCharacterRankingResult ranking,
        GetCharacterUnionResult union,
        GetCharacterSymbolResult symbols,
        GetCharacterSkillsResult skills,
        GetCharacterHexaResult hexa,
        GetCharacterEquipmentResult equipment
) {

    public static GetCharacterSummaryResult of(
            GetCharacterBasicResult basic,
            GetCharacterStatResult stat,
            GetCharacterRankingResult ranking,
            GetCharacterUnionResult union,
            GetCharacterSymbolResult symbols,
            GetCharacterSkillsResult skills,
            GetCharacterHexaResult hexa,
            GetCharacterEquipmentResult equipment
    ) {
        return new GetCharacterSummaryResult(
                basic,
                stat,
                ranking,
                union,
                symbols,
                skills,
                hexa,
                equipment
        );
    }
}
