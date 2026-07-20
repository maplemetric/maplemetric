package com.maplemetric.character.infrastructure.client.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CharacterHexaMatrixResponse(
        String date,
        List<HexaCore> characterHexaCoreEquipment
) {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record HexaCore(
            String hexaCoreName,
            Integer hexaCoreLevel,
            String hexaCoreType,
            List<LinkedSkill> linkedSkill
    ) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record LinkedSkill(
            String hexaSkillId
    ) {
    }
}
