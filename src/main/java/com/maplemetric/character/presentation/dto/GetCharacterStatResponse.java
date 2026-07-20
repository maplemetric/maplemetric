package com.maplemetric.character.presentation.dto;

import com.maplemetric.character.application.result.GetCharacterStatResult;

import java.util.List;

public record GetCharacterStatResponse(
        String date,
        String characterClass,
        String combatPower,
        Integer remainAp,
        List<FinalStatResponse> finalStat
) {

    public static GetCharacterStatResponse from(
            GetCharacterStatResult result
    ) {
        List<FinalStatResponse> finalStatResponses =
                result.finalStat().stream()
                        .map(finalStat ->
                                FinalStatResponse.from(finalStat)
                        )
                        .toList();

        return new GetCharacterStatResponse(
                result.date(),
                result.characterClass(),
                result.combatPower(),
                result.remainAp(),
                finalStatResponses
        );
    }

    public record FinalStatResponse(
            String statName,
            String statValue
    ) {

        public static FinalStatResponse from(
                GetCharacterStatResult.FinalStatResult result
        ) {
            return new FinalStatResponse(
                    result.statName(),
                    result.statValue()
            );
        }
    }
}