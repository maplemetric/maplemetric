package com.maplemetric.character.infrastructure.client.nexon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.maplemetric.character.application.port.out.LoadCharacterSetEffectPort.CharacterSetEffect;
import com.maplemetric.character.application.port.out.LoadCharacterSetEffectPort.SetEffect;
import com.maplemetric.character.application.port.out.LoadCharacterSetEffectPort.SetOption;
import com.maplemetric.character.infrastructure.client.CharacterClient;
import com.maplemetric.character.infrastructure.client.dto.CharacterSetEffectResponse;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NexonCharacterSetEffectAdapterTest {

    private static final String OCID = "test-ocid";

    @Mock
    private CharacterClient characterClient;

    @Test
    void Nexon세트효과응답을Application세트효과정보로변환한다() {
        NexonCharacterSetEffectAdapter adapter = createAdapter();

        given(characterClient.getCharacterSetEffect(OCID))
                .willReturn(
                        new CharacterSetEffectResponse(
                                "2026-07-23T00:00+09:00",
                                Arrays.asList(
                                        createResponseSetEffect(),
                                        null
                                )
                        )
                );

        CharacterSetEffect setEffect =
                adapter.loadCharacterSetEffect(OCID);

        assertThat(setEffect.setEffects())
                .containsExactly(
                        new SetEffect(
                                "여명의 보스 세트",
                                2,
                                List.of(
                                        new SetOption(
                                                2,
                                                "보스 몬스터 공격 시 데미지 : +10%"
                                        )
                                ),
                                List.of(
                                        new SetOption(
                                                2,
                                                "보스 몬스터 공격 시 데미지 : +10%"
                                        ),
                                        new SetOption(
                                                3,
                                                "올스탯 : +20"
                                        )
                                )
                        ),
                        null
                );

        verify(characterClient).getCharacterSetEffect(OCID);
        verifyNoMoreInteractions(characterClient);
    }

    @Test
    void Nexon세트효과목록이null이면null을유지한다() {
        NexonCharacterSetEffectAdapter adapter = createAdapter();

        given(characterClient.getCharacterSetEffect(OCID))
                .willReturn(
                        new CharacterSetEffectResponse(null, null)
                );

        CharacterSetEffect setEffect =
                adapter.loadCharacterSetEffect(OCID);

        assertThat(setEffect.setEffects())
                .isNull();

        verify(characterClient).getCharacterSetEffect(OCID);
        verifyNoMoreInteractions(characterClient);
    }

    private CharacterSetEffectResponse.SetEffect createResponseSetEffect() {
        return new CharacterSetEffectResponse.SetEffect(
                "여명의 보스 세트",
                2,
                List.of(
                        new CharacterSetEffectResponse.SetEffectInfo(
                                2,
                                "보스 몬스터 공격 시 데미지 : +10%"
                        )
                ),
                List.of(
                        new CharacterSetEffectResponse.SetEffectInfo(
                                2,
                                "보스 몬스터 공격 시 데미지 : +10%"
                        ),
                        new CharacterSetEffectResponse.SetEffectInfo(
                                3,
                                "올스탯 : +20"
                        )
                )
        );
    }

    private NexonCharacterSetEffectAdapter createAdapter() {
        return new NexonCharacterSetEffectAdapter(characterClient);
    }
}
