package com.maplemetric.character.infrastructure.client.nexon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.maplemetric.character.application.port.out.LoadCharacterAbilityPort.AbilityOption;
import com.maplemetric.character.application.port.out.LoadCharacterAbilityPort.CharacterAbility;
import com.maplemetric.character.infrastructure.client.CharacterClient;
import com.maplemetric.character.infrastructure.client.dto.CharacterAbilityResponse;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NexonCharacterAbilityAdapterTest {

    private static final String OCID = "test-ocid";

    @Mock
    private CharacterClient characterClient;

    @Test
    void Nexon어빌리티응답을Application어빌리티정보로변환한다() {
        NexonCharacterAbilityAdapter adapter =
                new NexonCharacterAbilityAdapter(characterClient);

        CharacterAbilityResponse.AbilityInfo abilityInfo =
                new CharacterAbilityResponse.AbilityInfo(
                        "1",
                        "레전드리",
                        "보스 몬스터 공격 시 데미지 20% 증가"
                );

        given(characterClient.getCharacterAbility(OCID))
                .willReturn(
                        new CharacterAbilityResponse(
                                "2026-07-23T00:00+09:00",
                                "레전드리",
                                Arrays.asList(abilityInfo, null),
                                100L,
                                1,
                                new CharacterAbilityResponse.AbilityPreset(
                                        "레전드리",
                                        List.of(abilityInfo)
                                ),
                                null,
                                new CharacterAbilityResponse.AbilityPreset(
                                        "에픽",
                                        null
                                )
                        )
                );

        CharacterAbility ability =
                adapter.loadCharacterAbility(OCID);

        assertThat(ability.date())
                .isEqualTo("2026-07-23T00:00+09:00");
        assertThat(ability.currentGrade())
                .isEqualTo("레전드리");
        assertThat(ability.currentOptions())
                .containsExactly(
                        new AbilityOption(
                                "1",
                                "레전드리",
                                "보스 몬스터 공격 시 데미지 20% 증가"
                        ),
                        null
                );
        assertThat(ability.remainFame())
                .isEqualTo(100L);
        assertThat(ability.appliedPresetNo())
                .isEqualTo(1);
        assertThat(ability.preset1().grade())
                .isEqualTo("레전드리");
        assertThat(ability.preset1().options())
                .containsExactly(
                        new AbilityOption(
                                "1",
                                "레전드리",
                                "보스 몬스터 공격 시 데미지 20% 증가"
                        )
                );
        assertThat(ability.preset2())
                .isNull();
        assertThat(ability.preset3().grade())
                .isEqualTo("에픽");
        assertThat(ability.preset3().options())
                .isNull();

        verify(characterClient).getCharacterAbility(OCID);
        verifyNoMoreInteractions(characterClient);
    }
}
