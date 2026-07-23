package com.maplemetric.character.application.result;

import com.maplemetric.character.application.port.out.LoadCharacterAbilityPort.AbilityOption;
import com.maplemetric.character.application.port.out.LoadCharacterAbilityPort.AbilityPreset;
import com.maplemetric.character.application.port.out.LoadCharacterAbilityPort.CharacterAbility;
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
            CharacterAbility ability
    ) {
        return new GetCharacterAbilityResult(
                ability.date(),
                ability.currentGrade(),
                convertOptions(ability.currentOptions()),
                ability.remainFame(),
                ability.appliedPresetNo(),
                List.of(
                        createPreset(1, ability.preset1()),
                        createPreset(2, ability.preset2()),
                        createPreset(3, ability.preset3())
                )
        );
    }

    private static AbilityPresetResult createPreset(
            int presetNo,
            AbilityPreset preset
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
                preset.grade(),
                convertOptions(preset.options())
        );
    }

    private static List<AbilityOptionResult> convertOptions(
            List<AbilityOption> options
    ) {
        if (options == null) {
            return List.of();
        }

        return options.stream()
                .filter(option -> option != null)
                .map(option -> new AbilityOptionResult(
                        option.optionNo(),
                        option.grade(),
                        option.value()
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
