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
import java.util.function.Function;
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
                convertList(
                        vMatrixResponse.characterVCoreEquipment(),
                        core -> new VCore(
                                core.vCoreName(),
                                core.vCoreType(),
                                core.vCoreLevel()
                        )
                ),
                convertList(
                        fifthSkillResponse.characterSkill(),
                        skill -> new FifthSkill(
                                skill.skillName(),
                                skill.skillIcon(),
                                skill.skillDescription(),
                                skill.skillEffect(),
                                skill.skillEffectNext()
                        )
                ),
                convertList(
                        linkSkillResponse.characterLinkSkill(),
                        skill -> toLinkSkill(skill)
                ),
                convertList(
                        linkSkillResponse.characterLinkSkillPreset1(),
                        skill -> toLinkSkill(skill)
                ),
                convertList(
                        linkSkillResponse.characterLinkSkillPreset2(),
                        skill -> toLinkSkill(skill)
                ),
                convertList(
                        linkSkillResponse.characterLinkSkillPreset3(),
                        skill -> toLinkSkill(skill)
                )
        );
    }

    private LinkSkill toLinkSkill(
            CharacterLinkSkillResponse.LinkSkill skill
    ) {
        return new LinkSkill(
                skill.skillName(),
                skill.skillLevel(),
                skill.skillIcon(),
                skill.skillDescription(),
                skill.skillEffect(),
                skill.skillEffectNext()
        );
    }

    private <S, T> List<T> convertList(
            List<S> source,
            Function<S, T> mapper
    ) {
        if (source == null) {
            return null;
        }

        return source.stream()
                .map(item -> item == null
                        ? null
                        : mapper.apply(item))
                .toList();
    }
}
