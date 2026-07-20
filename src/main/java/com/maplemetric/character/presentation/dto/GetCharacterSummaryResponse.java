package com.maplemetric.character.presentation.dto;

import com.maplemetric.character.application.result.GetCharacterSummaryResult;

public record GetCharacterSummaryResponse(
        GetCharacterBasicResponse basic,
        GetCharacterEquipmentResponse equipment
) {

    public static GetCharacterSummaryResponse from(
            GetCharacterSummaryResult result
    ) {
        return new GetCharacterSummaryResponse(
                GetCharacterBasicResponse.from(result.basic()),
                GetCharacterEquipmentResponse.from(result.equipment())
        );
    }
}
