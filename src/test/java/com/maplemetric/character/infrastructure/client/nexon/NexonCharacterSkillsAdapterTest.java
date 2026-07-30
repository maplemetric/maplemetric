package com.maplemetric.character.infrastructure.client.nexon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.maplemetric.character.application.port.out.LoadCharacterSkillsPort.CharacterSkills;
import com.maplemetric.character.application.port.out.LoadCharacterSkillsPort.FifthSkill;
import com.maplemetric.character.application.port.out.LoadCharacterSkillsPort.LinkSkill;
import com.maplemetric.character.application.port.out.LoadCharacterSkillsPort.VCore;
import com.maplemetric.character.infrastructure.client.CharacterClient;
import com.maplemetric.character.infrastructure.client.dto.CharacterLinkSkillResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterSkillResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterVMatrixResponse;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NexonCharacterSkillsAdapterTest {

    private static final String OCID = "test-ocid";

    @Mock
    private CharacterClient characterClient;

    @Test
    void Nexon스킬응답을Application스킬정보로변환한다() {
        NexonCharacterSkillsAdapter adapter =
                new NexonCharacterSkillsAdapter(characterClient);

        CharacterVMatrixResponse.VCore vCore =
                new CharacterVMatrixResponse.VCore(
                        "조커",
                        "직업 코어",
                        30
                );

        given(characterClient.getCharacterVMatrix(OCID))
                .willReturn(
                        new CharacterVMatrixResponse(
                                null,
                                "팬텀",
                                Arrays.asList(vCore, null)
                        )
                );

        CharacterSkillResponse.Skill fifthSkill =
                new CharacterSkillResponse.Skill(
                        "조커",
                        30,
                        "https://example.com/joker.png",
                        "조커를 소환한다.",
                        "데미지 300%",
                        "데미지 320%"
                );

        given(characterClient.getCharacterSkill(OCID, "5"))
                .willReturn(
                        new CharacterSkillResponse(
                                null,
                                "팬텀",
                                "5",
                                Arrays.asList(fifthSkill, null)
                        )
                );

        CharacterLinkSkillResponse.LinkSkill linkSkill =
                new CharacterLinkSkillResponse.LinkSkill(
                        "데들리 인스팅트",
                        2,
                        "https://example.com/deadly-instinct.png",
                        "치명적인 일격을 노린다.",
                        "크리티컬 확률 10% 증가",
                        "크리티컬 확률 15% 증가"
                );

        given(characterClient.getCharacterLinkSkill(OCID))
                .willReturn(
                        new CharacterLinkSkillResponse(
                                null,
                                "팬텀",
                                List.of(linkSkill),
                                List.of(linkSkill),
                                null,
                                List.of()
                        )
                );

        CharacterSkills skills =
                adapter.loadCharacterSkills(OCID);

        assertThat(skills.vCores())
                .containsExactly(
                        new VCore(
                                "조커",
                                "직업 코어",
                                30
                        ),
                        null
                );

        assertThat(skills.fifthSkills())
                .containsExactly(
                        new FifthSkill(
                                "조커",
                                "https://example.com/joker.png",
                                "조커를 소환한다.",
                                "데미지 300%",
                                "데미지 320%"
                        ),
                        null
                );

        assertThat(skills.currentLinkSkills())
                .containsExactly(
                        new LinkSkill(
                                "데들리 인스팅트",
                                2,
                                "https://example.com/deadly-instinct.png",
                                "치명적인 일격을 노린다.",
                                "크리티컬 확률 10% 증가",
                                "크리티컬 확률 15% 증가"
                        )
                );

        assertThat(skills.linkSkillPreset1())
                .containsExactly(
                        new LinkSkill(
                                "데들리 인스팅트",
                                2,
                                "https://example.com/deadly-instinct.png",
                                "치명적인 일격을 노린다.",
                                "크리티컬 확률 10% 증가",
                                "크리티컬 확률 15% 증가"
                        )
                );

        assertThat(skills.linkSkillPreset2())
                .isNull();

        assertThat(skills.linkSkillPreset3())
                .isEmpty();

        verify(characterClient).getCharacterVMatrix(OCID);
        verify(characterClient).getCharacterSkill(OCID, "5");
        verify(characterClient).getCharacterLinkSkill(OCID);
        verifyNoMoreInteractions(characterClient);
    }
}
