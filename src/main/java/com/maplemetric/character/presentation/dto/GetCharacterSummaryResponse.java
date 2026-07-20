package com.maplemetric.character.presentation.dto;

import com.maplemetric.character.application.result.GetCharacterSummaryResult;

public record GetCharacterSummaryResponse(
        GetCharacterBasicResponse basic,
        GetCharacterStatResponse stat,
        GetCharacterUnionResponse union,
        GetCharacterSymbolResponse symbols,
        GetCharacterEquipmentResponse equipment
) {

    public static GetCharacterSummaryResponse from(
            GetCharacterSummaryResult result
    ) {
        return new GetCharacterSummaryResponse(
                GetCharacterBasicResponse.from(result.basic()),
                GetCharacterStatResponse.from(result.stat()),
                GetCharacterUnionResponse.from(result.union()),
                GetCharacterSymbolResponse.from(result.symbols()),
                GetCharacterEquipmentResponse.from(result.equipment())
        );
    }
}
