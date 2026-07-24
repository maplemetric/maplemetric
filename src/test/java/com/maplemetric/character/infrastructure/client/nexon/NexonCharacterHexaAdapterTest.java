package com.maplemetric.character.infrastructure.client.nexon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.maplemetric.character.application.port.out.LoadCharacterHexaPort.CharacterHexa;
import com.maplemetric.character.application.port.out.LoadCharacterHexaPort.HexaCore;
import com.maplemetric.character.application.port.out.LoadCharacterHexaPort.HexaStatCore;
import com.maplemetric.character.application.port.out.LoadCharacterHexaPort.LinkedSkill;
import com.maplemetric.character.application.port.out.LoadCharacterHexaPort.SixthSkill;
import com.maplemetric.character.infrastructure.client.CharacterClient;
import com.maplemetric.character.infrastructure.client.dto.CharacterHexaMatrixResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterHexaMatrixStatResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterSkillResponse;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NexonCharacterHexaAdapterTest {

    private static final String OCID = "test-ocid";

    @Mock
    private CharacterClient characterClient;

    @Test
    void Nexon헥사응답을Application헥사정보로변환한다() {
        NexonCharacterHexaAdapter adapter =
                new NexonCharacterHexaAdapter(characterClient);

        CharacterHexaMatrixResponse.LinkedSkill linkedSkill =
                new CharacterHexaMatrixResponse.LinkedSkill(
                        "템페스트 오브 카드 VI"
                );

        CharacterHexaMatrixResponse.HexaCore hexaCore =
                new CharacterHexaMatrixResponse.HexaCore(
                        "템페스트 오브 카드 VI",
                        18,
                        "마스터리 코어",
                        Arrays.asList(linkedSkill, null)
                );

        given(characterClient.getCharacterHexaMatrix(OCID))
                .willReturn(
                        new CharacterHexaMatrixResponse(
                                null,
                                Arrays.asList(hexaCore, null)
                        )
                );

        CharacterHexaMatrixStatResponse.HexaStatCore statCore =
                new CharacterHexaMatrixStatResponse.HexaStatCore(
                        "0",
                        "크리티컬 데미지 증가",
                        "공격력 증가",
                        "주력 스탯 증가",
                        4,
                        8,
                        8
                );

        given(characterClient.getCharacterHexaMatrixStat(OCID))
                .willReturn(
                        new CharacterHexaMatrixStatResponse(
                                null,
                                "팬텀",
                                List.of(statCore),
                                null,
                                List.of()
                        )
                );

        CharacterSkillResponse.Skill sixthSkill =
                new CharacterSkillResponse.Skill(
                        "템페스트 오브 카드 VI",
                        18,
                        "https://example.com/tempest-vi.png"
                );

        given(characterClient.getCharacterSkill(OCID, "6"))
                .willReturn(
                        new CharacterSkillResponse(
                                null,
                                "팬텀",
                                "6",
                                Arrays.asList(sixthSkill, null)
                        )
                );

        CharacterHexa hexa =
                adapter.loadCharacterHexa(OCID);

        assertThat(hexa.hexaCoreEquipment())
                .containsExactly(
                        new HexaCore(
                                "템페스트 오브 카드 VI",
                                "마스터리 코어",
                                18,
                                Arrays.asList(
                                        new LinkedSkill(
                                                "템페스트 오브 카드 VI"
                                        ),
                                        null
                                )
                        ),
                        null
                );

        assertThat(hexa.sixthSkills())
                .containsExactly(
                        new SixthSkill(
                                "템페스트 오브 카드 VI",
                                "https://example.com/tempest-vi.png"
                        ),
                        null
                );

        assertThat(hexa.hexaStatCore1())
                .containsExactly(
                        new HexaStatCore(
                                "0",
                                "크리티컬 데미지 증가",
                                4,
                                "공격력 증가",
                                8,
                                "주력 스탯 증가",
                                8
                        )
                );

        assertThat(hexa.hexaStatCore2())
                .isNull();

        assertThat(hexa.hexaStatCore3())
                .isEmpty();

        verify(characterClient).getCharacterHexaMatrix(OCID);
        verify(characterClient).getCharacterHexaMatrixStat(OCID);
        verify(characterClient).getCharacterSkill(OCID, "6");
        verifyNoMoreInteractions(characterClient);
    }
}
