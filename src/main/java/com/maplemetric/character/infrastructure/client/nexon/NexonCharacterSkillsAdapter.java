package com.maplemetric.character.infrastructure.client.nexon;

import com.maplemetric.character.application.port.out.LoadCharacterSkillsPort;
import com.maplemetric.character.application.port.out.LoadCharacterSkillsPort.CharacterSkills;
import com.maplemetric.character.application.port.out.LoadCharacterSkillsPort.FifthSkill;
import com.maplemetric.character.application.port.out.LoadCharacterSkillsPort.LinkSkill;
import com.maplemetric.character.application.port.out.LoadCharacterSkillsPort.VCore;
import com.maplemetric.character.infrastructure.client.CharacterClient;
import com.maplemetric.character.infrastructure.client.dto.CharacterLinkSkillResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterSkillResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterVMatrixResponse;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
class NexonCharacterSkillsAdapter
        implements LoadCharacterSkillsPort {

    private static final String FIFTH_SKILL_GRADE = "5";

    private final CharacterClient characterClient;

    NexonCharacterSkillsAdapter(
            CharacterClient characterClient
    ) {
        this.characterClient = characterClient;
    }

    @Override
    public CharacterSkills loadCharacterSkills(
            String ocid
    ) {
        CharacterVMatrixResponse vMatrixResponse =
                characterClient.getCharacterVMatrix(ocid);

        CharacterSkillResponse fifthSkillResponse =
                characterClient.getCharacterSkill(
                        ocid,
                        FIFTH_SKILL_GRADE
                );

        CharacterLinkSkillResponse linkSkillResponse =
                characterClient.getCharacterLinkSkill(ocid);

        return new CharacterSkills(
                convertVCores(
                        vMatrixResponse.characterVCoreEquipment()
                ),
                convertFifthSkills(
                        fifthSkillResponse.characterSkill()
                ),
                convertLinkSkills(
                        linkSkillResponse.characterLinkSkill()
                ),
                convertLinkSkills(
                        linkSkillResponse.characterLinkSkillPreset1()
                ),
                convertLinkSkills(
                        linkSkillResponse.characterLinkSkillPreset2()
                ),
                convertLinkSkills(
                        linkSkillResponse.characterLinkSkillPreset3()
                )
        );
    }

    private List<VCore> convertVCores(
            List<CharacterVMatrixResponse.VCore> cores
    ) {
        if (cores == null) {
            return null;
        }

        return cores.stream()
                .map(core -> core == null
                        ? null
                        : new VCore(
                                core.vCoreName(),
                                core.vCoreType(),
                                core.vCoreLevel()
                        ))
                .toList();
    }

    private List<FifthSkill> convertFifthSkills(
            List<CharacterSkillResponse.Skill> skills
    ) {
        if (skills == null) {
            return null;
        }

        return skills.stream()
                .map(skill -> skill == null
                        ? null
                        : new FifthSkill(
                                skill.skillName(),
                                skill.skillIcon()
                        ))
                .toList();
    }

    private List<LinkSkill> convertLinkSkills(
            List<CharacterLinkSkillResponse.LinkSkill> skills
    ) {
        if (skills == null) {
            return null;
        }

        return skills.stream()
                .map(skill -> skill == null
                        ? null
                        : new LinkSkill(
                                skill.skillName(),
                                skill.skillLevel(),
                                skill.skillIcon()
                        ))
                .toList();
    }
}
