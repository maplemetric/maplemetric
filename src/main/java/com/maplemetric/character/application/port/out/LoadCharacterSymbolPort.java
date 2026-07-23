package com.maplemetric.character.application.port.out;

import java.util.List;

public interface LoadCharacterSymbolPort {

    CharacterSymbol loadCharacterSymbol(String ocid);

    record CharacterSymbol(
            List<Symbol> symbols
    ) {
    }

    record Symbol(
            String symbolName,
            Integer symbolLevel,
            String symbolIcon
    ) {
    }
}
