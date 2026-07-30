package com.maplemetric.character.infrastructure.client.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CharacterSkillResponse(
        String date,
        String characterClass,
        String characterSkillGrade,
        List<Skill> characterSkill
) {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Skill(
            String skillName,
            Integer skillLevel,
            String skillIcon,
            String skillDescription,
            String skillEffect,
            String skillEffectNext
    ) {
    }
}
