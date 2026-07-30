package com.maplemetric.character.infrastructure.client.nexon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.maplemetric.character.application.port.out.LoadCharacterSymbolPort.CharacterSymbol;
import com.maplemetric.character.application.port.out.LoadCharacterSymbolPort.Symbol;
import com.maplemetric.character.infrastructure.client.CharacterClient;
import com.maplemetric.character.infrastructure.client.dto.CharacterSymbolResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NexonCharacterSymbolAdapterTest {

    private static final String OCID = "test-ocid";

    @Mock
    private CharacterClient characterClient;

    @Test
    void Nexon심볼응답을Application심볼정보로변환한다() {
        NexonCharacterSymbolAdapter adapter = createAdapter();

        given(characterClient.getCharacterSymbol(OCID))
                .willReturn(
                        new CharacterSymbolResponse(
                                "2026-07-23T00:00+09:00",
                                "팬텀",
                                List.of(
                                        createResponseSymbol(
                                                "아케인심볼 : 소멸의 여로",
                                                20,
                                                "https://example.com/vanishing.png"
                                        ),
                                        createResponseSymbol(
                                                "어센틱심볼 : 세르니움",
                                                11,
                                                "https://example.com/cernium.png"
                                        )
                                )
                        )
                );

        CharacterSymbol characterSymbol =
                adapter.loadCharacterSymbol(OCID);

        assertThat(characterSymbol.symbols())
                .containsExactly(
                        createExpectedSymbol(
                                "아케인심볼 : 소멸의 여로",
                                20,
                                "https://example.com/vanishing.png"
                        ),
                        createExpectedSymbol(
                                "어센틱심볼 : 세르니움",
                                11,
                                "https://example.com/cernium.png"
                        )
                );

        verify(characterClient).getCharacterSymbol(OCID);
        verifyNoMoreInteractions(characterClient);
    }

    @Test
    void Nexon심볼목록이null이면null을유지한다() {
        NexonCharacterSymbolAdapter adapter = createAdapter();

        given(characterClient.getCharacterSymbol(OCID))
                .willReturn(
                        new CharacterSymbolResponse(
                                null,
                                "팬텀",
                                null
                        )
                );

        CharacterSymbol characterSymbol =
                adapter.loadCharacterSymbol(OCID);

        assertThat(characterSymbol.symbols())
                .isNull();

        verify(characterClient).getCharacterSymbol(OCID);
        verifyNoMoreInteractions(characterClient);
    }

    private CharacterSymbolResponse.Symbol createResponseSymbol(
            String symbolName,
            Integer symbolLevel,
            String symbolIcon
    ) {
        return new CharacterSymbolResponse.Symbol(
                symbolName,
                symbolIcon,
                "테스트 설명",
                "테스트 추가 효과",
                "530",
                symbolLevel,
                "2200",
                "0",
                "0",
                "0",
                "0",
                "1",
                "2",
                "3",
                2678,
                4000
        );
    }

    private Symbol createExpectedSymbol(
            String symbolName,
            Integer symbolLevel,
            String symbolIcon
    ) {
        return new Symbol(
                symbolName,
                symbolIcon,
                "테스트 설명",
                "테스트 추가 효과",
                "530",
                symbolLevel,
                "2200",
                "0",
                "0",
                "0",
                "0",
                "1",
                "2",
                "3",
                2678,
                4000
        );
    }

    private NexonCharacterSymbolAdapter createAdapter() {
        return new NexonCharacterSymbolAdapter(characterClient);
    }
}
