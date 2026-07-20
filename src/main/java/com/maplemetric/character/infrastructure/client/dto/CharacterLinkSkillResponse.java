package com.maplemetric.character.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CharacterLinkSkillResponse(
        String date,
        String characterClass,
        List<LinkSkill> characterLinkSkill,

        @JsonProperty("character_link_skill_preset_1")
        List<LinkSkill> characterLinkSkillPreset1,

        @JsonProperty("character_link_skill_preset_2")
        List<LinkSkill> characterLinkSkillPreset2,

        @JsonProperty("character_link_skill_preset_3")
        List<LinkSkill> characterLinkSkillPreset3
) {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record LinkSkill(
            String skillName,
            Integer skillLevel,
            String skillIcon
    ) {
    }
}
