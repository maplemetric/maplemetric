package com.maplemetric.character.application.result;

public record GetCharacterSummaryResult(
        GetCharacterBasicResult basic,
        GetCharacterStatResult stat,
        GetCharacterUnionResult union,
        GetCharacterSymbolResult symbols,
        GetCharacterEquipmentResult equipment
) {

    public static GetCharacterSummaryResult of(
            GetCharacterBasicResult basic,
            GetCharacterStatResult stat,
            GetCharacterUnionResult union,
            GetCharacterSymbolResult symbols,
            GetCharacterEquipmentResult equipment
    ) {
        return new GetCharacterSummaryResult(
                basic,
                stat,
                union,
                symbols,
                equipment
        );
    }
}
