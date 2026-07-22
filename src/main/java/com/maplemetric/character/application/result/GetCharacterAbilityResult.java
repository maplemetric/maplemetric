package com.maplemetric.character.application.result;

import com.maplemetric.character.infrastructure.client.dto.CharacterAbilityResponse;
import java.util.List;

public record GetCharacterAbilityResult(
        String date,
        String currentGrade,
        List<AbilityOptionResult> currentOptions,
        Long remainFame,
        Integer appliedPresetNo,
        List<AbilityPresetResult> presets
) {

    public static GetCharacterAbilityResult from(
            CharacterAbilityResponse response
    ) {
        return new GetCharacterAbilityResult(
                response.date(),
                response.abilityGrade(),
                convertOptions(response.abilityInfo()),
                response.remainFame(),
                response.presetNo(),
                List.of(
                        createPreset(1, response.abilityPreset1()),
                        createPreset(2, response.abilityPreset2()),
                        createPreset(3, response.abilityPreset3())
                )
        );
    }

    private static AbilityPresetResult createPreset(
            int presetNo,
            CharacterAbilityResponse.AbilityPreset preset
    ) {
        if (preset == null) {
            return new AbilityPresetResult(
                    presetNo,
                    null,
                    List.of()
            );
        }

        return new AbilityPresetResult(
                presetNo,
                preset.abilityPresetGrade(),
                convertOptions(preset.abilityInfo())
        );
    }

    private static List<AbilityOptionResult> convertOptions(
            List<CharacterAbilityResponse.AbilityInfo> options
    ) {
        if (options == null) {
            return List.of();
        }

        return options.stream()
                .filter(option -> option != null)
                .map(option -> new AbilityOptionResult(
                        option.abilityNo(),
                        option.abilityGrade(),
                        option.abilityValue()
                ))
                .toList();
    }

    public record AbilityPresetResult(
            Integer presetNo,
            String grade,
            List<AbilityOptionResult> options
    ) {
    }

    public record AbilityOptionResult(
            String optionNo,
            String grade,
            String value
    ) {
    }
}
