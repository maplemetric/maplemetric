package com.maplemetric.character.presentation.dto;

import com.maplemetric.character.application.result.GetCharacterAbilityResult;
import java.util.List;

public record GetCharacterAbilityResponse(
        String date,
        String currentGrade,
        List<AbilityOptionResponse> currentOptions,
        Long remainFame,
        Integer appliedPresetNo,
        List<AbilityPresetResponse> presets
) {

    public static GetCharacterAbilityResponse from(
            GetCharacterAbilityResult result
    ) {
        return new GetCharacterAbilityResponse(
                result.date(),
                result.currentGrade(),
                convertOptions(result.currentOptions()),
                result.remainFame(),
                result.appliedPresetNo(),
                result.presets().stream()
                        .map(preset -> new AbilityPresetResponse(
                                preset.presetNo(),
                                preset.grade(),
                                convertOptions(preset.options())
                        ))
                        .toList()
        );
    }

    private static List<AbilityOptionResponse> convertOptions(
            List<GetCharacterAbilityResult.AbilityOptionResult> options
    ) {
        return options.stream()
                .map(option -> new AbilityOptionResponse(
                        option.optionNo(),
                        option.grade(),
                        option.value()
                ))
                .toList();
    }

    public record AbilityPresetResponse(
            Integer presetNo,
            String grade,
            List<AbilityOptionResponse> options
    ) {
    }

    public record AbilityOptionResponse(
            String optionNo,
            String grade,
            String value
    ) {
    }
}
