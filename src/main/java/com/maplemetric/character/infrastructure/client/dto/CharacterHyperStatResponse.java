package com.maplemetric.character.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CharacterHyperStatResponse(
        String date,
        String characterClass,
        String usePresetNo,
        Long useAvailableHyperStat,

        @JsonProperty("hyper_stat_preset_1")
        List<HyperStat> hyperStatPreset1,

        @JsonProperty("hyper_stat_preset_1_remain_point")
        Long hyperStatPreset1RemainPoint,

        @JsonProperty("hyper_stat_preset_2")
        List<HyperStat> hyperStatPreset2,

        @JsonProperty("hyper_stat_preset_2_remain_point")
        Long hyperStatPreset2RemainPoint,

        @JsonProperty("hyper_stat_preset_3")
        List<HyperStat> hyperStatPreset3,

        @JsonProperty("hyper_stat_preset_3_remain_point")
        Long hyperStatPreset3RemainPoint
) {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record HyperStat(
            String statType,
            Long statPoint,
            Integer statLevel,
            String statIncrease
    ) {
    }
}
