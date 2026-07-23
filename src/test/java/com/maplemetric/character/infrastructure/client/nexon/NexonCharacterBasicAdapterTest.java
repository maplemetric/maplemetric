package com.maplemetric.character.infrastructure.client.nexon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.maplemetric.character.application.port.out.LoadCharacterBasicPort.CharacterBasic;
import com.maplemetric.character.infrastructure.client.CharacterClient;
import com.maplemetric.character.infrastructure.client.dto.CharacterBasicResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NexonCharacterBasicAdapterTest {

    private static final String CHARACTER_NAME = "감점";
    private static final String OCID = "test-ocid";

    @Mock
    private CharacterClient characterClient;

    @Test
    void 캐릭터명으로OCID를조회한다() {
        NexonCharacterBasicAdapter adapter = createAdapter();

        given(characterClient.getOcid(CHARACTER_NAME))
                .willReturn(OCID);

        assertThat(adapter.resolveOcid(CHARACTER_NAME))
                .isEqualTo(OCID);

        verify(characterClient).getOcid(CHARACTER_NAME);
        verifyNoMoreInteractions(characterClient);
    }

    @Test
    void Nexon기본응답을Application기본정보로변환한다() {
        NexonCharacterBasicAdapter adapter = createAdapter();

        given(characterClient.getCharacterBasic(OCID))
                .willReturn(createResponse());

        CharacterBasic basic =
                adapter.loadCharacterBasic(OCID);

        assertThat(basic)
                .isEqualTo(
                        new CharacterBasic(
                                CHARACTER_NAME,
                                "루나",
                                "남",
                                "팬텀",
                                "6",
                                282,
                                1229455582005L,
                                "3.020",
                                "연의",
                                "https://example.com/character.png",
                                "2017-12-22T00:00+09:00",
                                "true",
                                null
                        )
                );

        verify(characterClient).getCharacterBasic(OCID);
        verifyNoMoreInteractions(characterClient);
    }

    private NexonCharacterBasicAdapter createAdapter() {
        return new NexonCharacterBasicAdapter(characterClient);
    }

    private CharacterBasicResponse createResponse() {
        return new CharacterBasicResponse(
                null,
                CHARACTER_NAME,
                "루나",
                "남",
                "팬텀",
                "6",
                282,
                1229455582005L,
                "3.020",
                "연의",
                "https://example.com/character.png",
                "2017-12-22T00:00+09:00",
                "true",
                null
        );
    }
}
