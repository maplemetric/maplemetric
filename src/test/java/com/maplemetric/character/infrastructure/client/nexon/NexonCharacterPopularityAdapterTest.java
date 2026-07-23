package com.maplemetric.character.infrastructure.client.nexon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.maplemetric.character.application.port.out.LoadCharacterPopularityPort.CharacterPopularity;
import com.maplemetric.character.infrastructure.client.CharacterClient;
import com.maplemetric.character.infrastructure.client.dto.CharacterPopularityResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NexonCharacterPopularityAdapterTest {

    private static final String OCID = "test-ocid";

    @Mock
    private CharacterClient characterClient;

    @Test
    void Nexon인기도응답을Application인기도정보로변환한다() {
        NexonCharacterPopularityAdapter adapter =
                new NexonCharacterPopularityAdapter(characterClient);

        given(characterClient.getCharacterPopularity(OCID))
                .willReturn(
                        new CharacterPopularityResponse(
                                "2026-07-23T00:00+09:00",
                                1234L
                        )
                );

        CharacterPopularity popularity =
                adapter.loadCharacterPopularity(OCID);

        assertThat(popularity)
                .isEqualTo(
                        new CharacterPopularity(
                                "2026-07-23T00:00+09:00",
                                1234L
                        )
                );

        verify(characterClient).getCharacterPopularity(OCID);
        verifyNoMoreInteractions(characterClient);
    }

    @Test
    void Nexon인기도응답의null값을유지한다() {
        NexonCharacterPopularityAdapter adapter =
                new NexonCharacterPopularityAdapter(characterClient);

        given(characterClient.getCharacterPopularity(OCID))
                .willReturn(
                        new CharacterPopularityResponse(
                                null,
                                null
                        )
                );

        CharacterPopularity popularity =
                adapter.loadCharacterPopularity(OCID);

        assertThat(popularity)
                .isEqualTo(
                        new CharacterPopularity(
                                null,
                                null
                        )
                );

        verify(characterClient).getCharacterPopularity(OCID);
        verifyNoMoreInteractions(characterClient);
    }
}
