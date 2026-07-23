package com.maplemetric.character.infrastructure.client.nexon;

import com.maplemetric.character.application.port.out.LoadCharacterSymbolPort;
import com.maplemetric.character.application.port.out.LoadCharacterSymbolPort.CharacterSymbol;
import com.maplemetric.character.infrastructure.client.CharacterClient;
import com.maplemetric.character.infrastructure.client.dto.CharacterSymbolResponse;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
class NexonCharacterSymbolAdapter
        implements LoadCharacterSymbolPort {

    private final CharacterClient characterClient;

    NexonCharacterSymbolAdapter(
            CharacterClient characterClient
    ) {
        this.characterClient = characterClient;
    }

    @Override
    public CharacterSymbol loadCharacterSymbol(
            String ocid
    ) {
        CharacterSymbolResponse response =
                characterClient.getCharacterSymbol(ocid);

        return new CharacterSymbol(
                convertSymbols(response)
        );
    }

    private List<LoadCharacterSymbolPort.Symbol> convertSymbols(
            CharacterSymbolResponse response
    ) {
        if (response.symbol() == null) {
            return null;
        }

        return response.symbol().stream()
                .map(symbol -> new LoadCharacterSymbolPort.Symbol(
                        symbol.symbolName(),
                        symbol.symbolLevel(),
                        symbol.symbolIcon()
                ))
                .toList();
    }
}
