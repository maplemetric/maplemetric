package com.maplemetric.character.infrastructure.client.nexon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.maplemetric.character.application.port.out.LoadCharacterHyperStatPort.CharacterHyperStat;
import com.maplemetric.character.application.port.out.LoadCharacterHyperStatPort.HyperStat;
import com.maplemetric.character.infrastructure.client.CharacterClient;
import com.maplemetric.character.infrastructure.client.dto.CharacterHyperStatResponse;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NexonCharacterHyperStatAdapterTest {

    private static final String OCID = "test-ocid";

    @Mock
    private CharacterClient characterClient;

    @Test
    void Nexon하이퍼스탯응답을Application하이퍼스탯정보로변환한다() {
        NexonCharacterHyperStatAdapter adapter =
                new NexonCharacterHyperStatAdapter(characterClient);

        given(characterClient.getCharacterHyperStat(OCID))
                .willReturn(
                        new CharacterHyperStatResponse(
                                "2026-07-23T00:00+09:00",
                                "팬텀",
                                "2",
                                5L,
                                List.of(
                                        new CharacterHyperStatResponse.HyperStat(
                                                "크리티컬 확률",
                                                15L,
                                                5,
                                                "크리티컬 확률 5% 증가"
                                        )
                                ),
                                10L,
                                null,
                                20L,
                                Arrays.asList(
                                        (CharacterHyperStatResponse.HyperStat) null
                                ),
                                30L
                        )
                );

        CharacterHyperStat hyperStat =
                adapter.loadCharacterHyperStat(OCID);

        assertThat(hyperStat.date())
                .isEqualTo("2026-07-23T00:00+09:00");
        assertThat(hyperStat.characterClass())
                .isEqualTo("팬텀");
        assertThat(hyperStat.appliedPresetNo())
                .isEqualTo("2");
        assertThat(hyperStat.availablePoints())
                .isEqualTo(5L);
        assertThat(hyperStat.preset1())
                .containsExactly(
                        new HyperStat(
                                "크리티컬 확률",
                                15L,
                                5,
                                "크리티컬 확률 5% 증가"
                        )
                );
        assertThat(hyperStat.preset1RemainPoints())
                .isEqualTo(10L);
        assertThat(hyperStat.preset2())
                .isNull();
        assertThat(hyperStat.preset2RemainPoints())
                .isEqualTo(20L);
        assertThat(hyperStat.preset3())
                .containsExactly((HyperStat) null);
        assertThat(hyperStat.preset3RemainPoints())
                .isEqualTo(30L);

        verify(characterClient).getCharacterHyperStat(OCID);
        verifyNoMoreInteractions(characterClient);
    }
}
