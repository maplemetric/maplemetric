package com.maplemetric.character.application.port.out;

import java.util.List;

public interface LoadCharacterSkillsPort {

    CharacterSkills loadCharacterSkills(String ocid);

    record CharacterSkills(
            List<VCore> vCores,
            List<FifthSkill> fifthSkills,
            List<LinkSkill> currentLinkSkills,
            List<LinkSkill> linkSkillPreset1,
            List<LinkSkill> linkSkillPreset2,
            List<LinkSkill> linkSkillPreset3
    ) {
    }

    record VCore(
            String vCoreName,
            String vCoreType,
            Integer vCoreLevel
    ) {
    }

    record FifthSkill(
            String skillName,
            String skillIcon
    ) {
    }

    record LinkSkill(
            String skillName,
            Integer skillLevel,
            String skillIcon
    ) {
    }
}
