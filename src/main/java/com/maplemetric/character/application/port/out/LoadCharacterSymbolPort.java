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
            String symbolIcon,
            String symbolDescription,
            String symbolOtherEffectDescription,
            String symbolForce,
            Integer symbolLevel,
            String symbolStr,
            String symbolDex,
            String symbolIntelligence,
            String symbolLuk,
            String symbolHp,
            String symbolDropRate,
            String symbolMesoRate,
            String symbolExpRate,
            Integer symbolGrowthCount,
            Integer symbolRequireGrowthCount
    ) {
    }
}
