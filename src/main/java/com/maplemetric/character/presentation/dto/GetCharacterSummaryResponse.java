package com.maplemetric.character.presentation.dto;

import com.maplemetric.character.application.result.GetCharacterSummaryResult;

public record GetCharacterSummaryResponse(
        GetCharacterBasicResponse basic,
        GetCharacterStatResponse stat,
        GetCharacterEquipmentResponse equipment
) {

    public static GetCharacterSummaryResponse from(
            GetCharacterSummaryResult result
    ) {
        return new GetCharacterSummaryResponse(
                GetCharacterBasicResponse.from(result.basic()),
                GetCharacterStatResponse.from(result.stat()),
                GetCharacterEquipmentResponse.from(result.equipment())
        );
    }
}