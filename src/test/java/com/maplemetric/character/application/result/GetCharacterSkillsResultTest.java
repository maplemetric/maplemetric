package com.maplemetric.character.application.result;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.maplemetric.character.infrastructure.client.dto.CharacterLinkSkillResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterSkillResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterVMatrixResponse;
import java.util.List;
import org.junit.jupiter.api.Test;

class GetCharacterSkillsResultTest {

    @Test
    void V코어이름과5차스킬이정확히일치하면아이콘을매핑한다() {
        CharacterVMatrixResponse vMatrixResponse =
                new CharacterVMatrixResponse(
                        null,
                        "팬텀",
                        List.of(
                                new CharacterVMatrixResponse.VCore(
                                        "조커",
                                        "직업 코어",
                                        30
                                ),
                                new CharacterVMatrixResponse.VCore(
                                        "조커 강화",
                                        "강화 코어",
                                        30
                                )
                        )
                );

        CharacterSkillResponse skillResponse =
                new CharacterSkillResponse(
                        null,
                        "팬텀",
                        "5",
                        List.of(
                                new CharacterSkillResponse.Skill(
                                        "조커",
                                        30,
                                        "first-icon"
                                ),
                                new CharacterSkillResponse.Skill(
                                        "조커",
                                        30,
                                        "second-icon"
                                ),
                                new CharacterSkillResponse.Skill(
                                        " ",
                                        1,
                                        "blank-name-icon"
                                )
                        )
                );

        GetCharacterSkillsResult result =
                GetCharacterSkillsResult.of(
                        vMatrixResponse,
                        skillResponse,
                        createEmptyLinkSkillResponse()
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
        CharacterVMatrixResponse vMatrixResponse =
                new CharacterVMatrixResponse(
                        null,
                        "팬텀",
                        List.of(
                                new CharacterVMatrixResponse.VCore(
                                        "조커",
                                        "직업 코어",
                                        30
                                )
                        )
                );

        CharacterSkillResponse skillResponse =
                new CharacterSkillResponse(
                        null,
                        "팬텀",
                        "5",
                        List.of(
                                new CharacterSkillResponse.Skill(
                                        "조커",
                                        30,
                                        null
                                )
                        )
                );

        GetCharacterSkillsResult result =
                GetCharacterSkillsResult.of(
                        vMatrixResponse,
                        skillResponse,
                        createEmptyLinkSkillResponse()
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
        CharacterLinkSkillResponse.LinkSkill skillA =
                createLinkSkill("스킬A", 2);

        CharacterLinkSkillResponse.LinkSkill skillB =
                createLinkSkill("스킬B", 1);

        CharacterLinkSkillResponse response =
                new CharacterLinkSkillResponse(
                        null,
                        "팬텀",
                        List.of(skillA, skillA, skillB),
                        List.of(skillB, skillA, skillA),
                        List.of(skillA, skillB),
                        List.of(skillA, skillA, skillB)
                );

        GetCharacterSkillsResult result =
                GetCharacterSkillsResult.of(
                        new CharacterVMatrixResponse(
                                null,
                                "팬텀",
                                List.of()
                        ),
                        new CharacterSkillResponse(
                                null,
                                "팬텀",
                                "5",
                                List.of()
                        ),
                        response
                );

        assertThat(result.linkSkills().matchedPresetNos())
                .containsExactly(1, 3);

        assertThat(result.linkSkills().presets())
                .extracting(preset -> preset.presetNo())
                .containsExactly(1, 2, 3);
    }

    @Test
    void 링크스킬과일치하는프리셋이없으면빈목록을반환한다() {
        CharacterLinkSkillResponse response =
                new CharacterLinkSkillResponse(
                        null,
                        "팬텀",
                        List.of(createLinkSkill("현재 스킬", 2)),
                        null,
                        List.of(createLinkSkill("다른 스킬", 2)),
                        List.of()
                );

        GetCharacterSkillsResult result =
                GetCharacterSkillsResult.of(
                        new CharacterVMatrixResponse(
                                null,
                                "팬텀",
                                null
                        ),
                        new CharacterSkillResponse(
                                null,
                                "팬텀",
                                "5",
                                null
                        ),
                        response
                );

        assertThat(result.vMatrix().cores()).isEmpty();
        assertThat(result.linkSkills().matchedPresetNos()).isEmpty();
        assertThat(result.linkSkills().presets()).hasSize(3);
    }

    private CharacterLinkSkillResponse createEmptyLinkSkillResponse() {
        return new CharacterLinkSkillResponse(
                null,
                "팬텀",
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }

    private CharacterLinkSkillResponse.LinkSkill createLinkSkill(
            String skillName,
            int skillLevel
    ) {
        return new CharacterLinkSkillResponse.LinkSkill(
                skillName,
                skillLevel,
                skillName + "-icon"
        );
    }
}
