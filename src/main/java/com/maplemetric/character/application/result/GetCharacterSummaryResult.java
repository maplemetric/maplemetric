package com.maplemetric.character.application.result;

public record GetCharacterSummaryResult(
        GetCharacterBasicResult basic,
        GetCharacterEquipmentResult equipment
) {

    public static GetCharacterSummaryResult of(
            GetCharacterBasicResult basic,
            GetCharacterEquipmentResult equipment
    ) {
        return new GetCharacterSummaryResult(basic, equipment);
    }
}
