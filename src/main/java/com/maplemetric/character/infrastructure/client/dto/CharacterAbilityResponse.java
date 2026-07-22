package com.maplemetric.character.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CharacterAbilityResponse(
        String date,
        String abilityGrade,
        List<AbilityInfo> abilityInfo,
        Long remainFame,
        Integer presetNo,

        @JsonProperty("ability_preset_1")
        AbilityPreset abilityPreset1,

        @JsonProperty("ability_preset_2")
        AbilityPreset abilityPreset2,

        @JsonProperty("ability_preset_3")
        AbilityPreset abilityPreset3
) {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record AbilityPreset(
            String abilityPresetGrade,
            List<AbilityInfo> abilityInfo
    ) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record AbilityInfo(
            String abilityNo,
            String abilityGrade,
            String abilityValue
    ) {
    }
}
