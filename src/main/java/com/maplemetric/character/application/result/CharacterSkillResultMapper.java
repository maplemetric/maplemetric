package com.maplemetric.character.application.result;

import com.maplemetric.character.infrastructure.client.dto.CharacterSkillResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;

final class CharacterSkillResultMapper {

    private CharacterSkillResultMapper() {
    }

    static Map<String, CharacterSkillResponse.Skill> createSkillMap(
            CharacterSkillResponse response
    ) {
        if (response == null
                || response.characterSkill() == null) {
            return Map.of();
        }

        return response.characterSkill()
                .stream()
                .filter(skill -> skill != null)
                .filter(skill -> StringUtils.hasText(
                        skill.skillName()
                ))
                .collect(Collectors.toMap(
                        skill -> skill.skillName(),
                        skill -> skill,
                        (firstSkill, ignoredSkill) -> firstSkill,
                        () -> new LinkedHashMap<>()
                ));
    }
}