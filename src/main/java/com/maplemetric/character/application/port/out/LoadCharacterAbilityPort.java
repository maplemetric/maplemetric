package com.maplemetric.character.application.port.out;

import java.util.List;

public interface LoadCharacterAbilityPort {

    CharacterAbility loadCharacterAbility(String ocid);

    record CharacterAbility(
            String date,
            String currentGrade,
            List<AbilityOption> currentOptions,
            Long remainFame,
            Integer appliedPresetNo,
            AbilityPreset preset1,
            AbilityPreset preset2,
            AbilityPreset preset3
    ) {
    }

    record AbilityPreset(
            String grade,
            List<AbilityOption> options
    ) {
    }

    record AbilityOption(
            String optionNo,
            String grade,
            String value
    ) {
    }
}
