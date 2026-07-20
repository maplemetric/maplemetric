package com.maplemetric.character.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CharacterVMatrixResponse(
        String date,
        String characterClass,

        @JsonProperty("character_v_core_equipment")
        List<VCore> characterVCoreEquipment
) {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record VCore(
            @JsonProperty("v_core_name")
            String vCoreName,

            @JsonProperty("v_core_type")
            String vCoreType,

            @JsonProperty("v_core_level")
            Integer vCoreLevel
    ) {
    }
}
