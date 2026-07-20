package com.maplemetric.character.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CharacterHexaMatrixStatResponse(
        String date,
        String characterClass,

        @JsonProperty("character_hexa_stat_core")
        List<HexaStatCore> characterHexaStatCore,

        @JsonProperty("character_hexa_stat_core_2")
        List<HexaStatCore> characterHexaStatCore2,

        @JsonProperty("character_hexa_stat_core_3")
        List<HexaStatCore> characterHexaStatCore3
) {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record HexaStatCore(
            String slotId,
            String mainStatName,

            @JsonProperty("sub_stat_name_1")
            String subStatName1,

            @JsonProperty("sub_stat_name_2")
            String subStatName2,

            Integer mainStatLevel,

            @JsonProperty("sub_stat_level_1")
            Integer subStatLevel1,

            @JsonProperty("sub_stat_level_2")
            Integer subStatLevel2
    ) {
    }
}
