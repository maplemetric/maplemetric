package com.maplemetric.character.infrastructure.client.nexon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.maplemetric.character.application.port.out.LoadCharacterStatPort;
import com.maplemetric.character.application.port.out.LoadCharacterStatPort.CharacterStat;
import com.maplemetric.character.infrastructure.client.CharacterClient;
import com.maplemetric.character.infrastructure.client.dto.CharacterStatResponse;
import com.maplemetric.character.infrastructure.client.dto.FinalStat;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NexonCharacterStatAdapterTest {

    private static final String OCID = "test-ocid";

    @Mock
    private CharacterClient characterClient;

    @Test
    void Nexon스탯응답을Application스탯정보로변환한다() {
        NexonCharacterStatAdapter adapter = createAdapter();

        given(characterClient.getCharacterStat(OCID))
                .willReturn(
                        new CharacterStatResponse(
                                "2026-07-23T00:00+09:00",
                                "팬텀",
                                List.of(
                                        new FinalStat(
                                                "전투력",
                                                "116871666"
                                        ),
                                        new FinalStat(
                                                "LUK",
                                                "50724"
                                        )
                                ),
                                3
                        )
                );

        CharacterStat stat =
                adapter.loadCharacterStat(OCID);

        assertThat(stat)
                .isEqualTo(
                        new CharacterStat(
                                "2026-07-23T00:00+09:00",
                                "팬텀",
                                List.of(
                                        new LoadCharacterStatPort.FinalStat(
                                                "전투력",
                                                "116871666"
                                        ),
                                        new LoadCharacterStatPort.FinalStat(
                                                "LUK",
                                                "50724"
                                        )
                                ),
                                3
                        )
                );

        verify(characterClient).getCharacterStat(OCID);
        verifyNoMoreInteractions(characterClient);
    }

    @Test
    void Nexon최종스탯목록이null이면null을유지한다() {
        NexonCharacterStatAdapter adapter = createAdapter();

        given(characterClient.getCharacterStat(OCID))
                .willReturn(
                        new CharacterStatResponse(
                                null,
                                "팬텀",
                                null,
                                0
                        )
                );

        CharacterStat stat =
                adapter.loadCharacterStat(OCID);

        assertThat(stat.finalStat())
                .isNull();

        verify(characterClient).getCharacterStat(OCID);
        verifyNoMoreInteractions(characterClient);
    }

    private NexonCharacterStatAdapter createAdapter() {
        return new NexonCharacterStatAdapter(characterClient);
    }
}
