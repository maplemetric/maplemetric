package com.maplemetric.character.application.result;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.maplemetric.character.application.port.out.LoadCharacterSkillsPort.CharacterSkills;
import com.maplemetric.character.application.port.out.LoadCharacterSkillsPort.FifthSkill;
import com.maplemetric.character.application.port.out.LoadCharacterSkillsPort.LinkSkill;
import com.maplemetric.character.application.port.out.LoadCharacterSkillsPort.VCore;
import java.util.List;
import org.junit.jupiter.api.Test;

class GetCharacterSkillsResultTest {

    @Test
    void V코어이름과5차스킬이정확히일치하면아이콘을매핑한다() {
        List<VCore> vCores = List.of(
                new VCore(
                        "조커",
                        "직업 코어",
                        30
                ),
                new VCore(
                        "조커 강화",
                        "강화 코어",
                        30
                )
        );

        List<FifthSkill> fifthSkills = List.of(
                new FifthSkill(
                        "조커",
                        "first-icon"
                ),
                new FifthSkill(
                        "조커",
                        "second-icon"
                ),
                new FifthSkill(
                        " ",
                        "blank-name-icon"
                )
        );

        GetCharacterSkillsResult result =
                GetCharacterSkillsResult.from(
                        createSkills(
                                vCores,
                                fifthSkills,
                                List.of(),
                                List.of(),
                                List.of(),
                                List.of()
                        )
                );

        assertThat(result.vMatrix().cores())
                .extracting(
                        core -> core.coreName(),
                        core -> core.skills().size()
                )
                .containsExactly(
                        tuple("조커", 1),
                        tuple("조커 강화", 0)
                );

        assertThat(result.vMatrix().cores().get(0).skills())
                .extracting(
                        skill -> skill.skillName(),
                        skill -> skill.skillIcon()
                )
                .containsExactly(
                        tuple("조커", "first-icon")
                );
    }

    @Test
    void 정확히일치한5차스킬의아이콘이없으면null을유지한다() {
        List<VCore> vCores = List.of(
                new VCore(
                        "조커",
                        "직업 코어",
                        30
                )
        );

        List<FifthSkill> fifthSkills = List.of(
                new FifthSkill(
                        "조커",
                        null
                )
        );

        GetCharacterSkillsResult result =
                GetCharacterSkillsResult.from(
                        createSkills(
                                vCores,
                                fifthSkills,
                                List.of(),
                                List.of(),
                                List.of(),
                                List.of()
                        )
                );

        assertThat(
                result.vMatrix()
                        .cores()
                        .get(0)
                        .skills()
                        .get(0)
                        .skillIcon()
        ).isNull();
    }

    @Test
    void 링크스킬순서를무시하고중복개수를유지하여일치프리셋을찾는다() {
        LinkSkill skillA =
                createLinkSkill("스킬A", 2);

        LinkSkill skillB =
                createLinkSkill("스킬B", 1);

        GetCharacterSkillsResult result =
                GetCharacterSkillsResult.from(
                        createSkills(
                                List.of(),
                                List.of(),
                                List.of(skillA, skillA, skillB),
                                List.of(skillB, skillA, skillA),
                                List.of(skillA, skillB),
                                List.of(skillA, skillA, skillB)
                        )
                );

        assertThat(result.linkSkills().matchedPresetNos())
                .containsExactly(1, 3);

        assertThat(result.linkSkills().presets())
                .extracting(preset -> preset.presetNo())
                .containsExactly(1, 2, 3);
    }

    @Test
    void 링크스킬과일치하는프리셋이없으면빈목록을반환한다() {
        CharacterSkills skills = new CharacterSkills(
                null,
                null,
                List.of(createLinkSkill("현재 스킬", 2)),
                null,
                List.of(createLinkSkill("다른 스킬", 2)),
                List.of()
        );

        GetCharacterSkillsResult result =
                GetCharacterSkillsResult.from(skills);

        assertThat(result.vMatrix().cores()).isEmpty();
        assertThat(result.linkSkills().matchedPresetNos()).isEmpty();
        assertThat(result.linkSkills().presets()).hasSize(3);
    }

    private CharacterSkills createSkills(
            List<VCore> vCores,
            List<FifthSkill> fifthSkills,
            List<LinkSkill> currentLinkSkills,
            List<LinkSkill> linkSkillPreset1,
            List<LinkSkill> linkSkillPreset2,
            List<LinkSkill> linkSkillPreset3
    ) {
        return new CharacterSkills(
                vCores,
                fifthSkills,
                currentLinkSkills,
                linkSkillPreset1,
                linkSkillPreset2,
                linkSkillPreset3
        );
    }

    private LinkSkill createLinkSkill(
            String skillName,
            int skillLevel
    ) {
        return new LinkSkill(
                skillName,
                skillLevel,
                skillName + "-icon"
        );
    }
}
