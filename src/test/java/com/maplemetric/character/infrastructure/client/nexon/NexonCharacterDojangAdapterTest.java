package com.maplemetric.character.infrastructure.client.nexon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.maplemetric.character.application.port.out.LoadCharacterDojangPort.CharacterDojang;
import com.maplemetric.character.infrastructure.client.CharacterClient;
import com.maplemetric.character.infrastructure.client.dto.CharacterDojangResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NexonCharacterDojangAdapterTest {

    private static final String OCID = "test-ocid";

    @Mock
    private CharacterClient characterClient;

    @Test
    void Nexon무릉응답을Application무릉정보로변환한다() {
        NexonCharacterDojangAdapter adapter =
                new NexonCharacterDojangAdapter(characterClient);

        given(characterClient.getCharacterDojang(OCID))
                .willReturn(
                        new CharacterDojangResponse(
                                "2026-07-23T00:00+09:00",
                                "팬텀",
                                "루나",
                                57,
                                "2026-07-22T00:00+09:00",
                                600
                        )
                );

        CharacterDojang dojang =
                adapter.loadCharacterDojang(OCID);

        assertThat(dojang)
                .isEqualTo(
                        new CharacterDojang(
                                "2026-07-23T00:00+09:00",
                                "팬텀",
                                "루나",
                                57,
                                "2026-07-22T00:00+09:00",
                                600
                        )
                );

        verify(characterClient).getCharacterDojang(OCID);
        verifyNoMoreInteractions(characterClient);
    }

    @Test
    void Nexon무릉응답의null값을유지한다() {
        NexonCharacterDojangAdapter adapter =
                new NexonCharacterDojangAdapter(characterClient);

        given(characterClient.getCharacterDojang(OCID))
                .willReturn(
                        new CharacterDojangResponse(
                                null,
                                null,
                                null,
                                null,
                                null,
                                null
                        )
                );

        CharacterDojang dojang =
                adapter.loadCharacterDojang(OCID);

        assertThat(dojang)
                .isEqualTo(
                        new CharacterDojang(
                                null,
                                null,
                                null,
                                null,
                                null,
                                null
                        )
                );

        verify(characterClient).getCharacterDojang(OCID);
        verifyNoMoreInteractions(characterClient);
    }
}
