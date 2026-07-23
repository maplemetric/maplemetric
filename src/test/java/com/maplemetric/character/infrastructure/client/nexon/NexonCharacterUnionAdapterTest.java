package com.maplemetric.character.infrastructure.client.nexon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.maplemetric.character.application.port.out.LoadCharacterUnionPort.CharacterUnion;
import com.maplemetric.character.infrastructure.client.CharacterClient;
import com.maplemetric.character.infrastructure.client.dto.CharacterUnionResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NexonCharacterUnionAdapterTest {

    private static final String OCID = "test-ocid";

    @Mock
    private CharacterClient characterClient;

    @Test
    void Nexon유니온응답을Application유니온정보로변환한다() {
        NexonCharacterUnionAdapter adapter =
                new NexonCharacterUnionAdapter(
                        characterClient
                );

        given(characterClient.getCharacterUnion(OCID))
                .willReturn(
                        new CharacterUnionResponse(
                                9000,
                                50
                        )
                );

        CharacterUnion union =
                adapter.loadCharacterUnion(OCID);

        assertThat(union)
                .isEqualTo(
                        new CharacterUnion(
                                9000,
                                50
                        )
                );

        verify(characterClient).getCharacterUnion(OCID);
        verifyNoMoreInteractions(characterClient);
    }
}
