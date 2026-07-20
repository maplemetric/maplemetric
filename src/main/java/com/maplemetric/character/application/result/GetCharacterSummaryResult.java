package com.maplemetric.character.application.result;

public record GetCharacterSummaryResult(
        GetCharacterBasicResult basic,
        GetCharacterStatResult stat,
        GetCharacterEquipmentResult equipment
) {

    public static GetCharacterSummaryResult of(
            GetCharacterBasicResult basic,
            GetCharacterStatResult stat,
            GetCharacterEquipmentResult equipment
    ) {
        return new GetCharacterSummaryResult(basic, stat, equipment);
    }
}