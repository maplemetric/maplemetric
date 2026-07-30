package com.maplemetric.character.presentation.dto;

import com.maplemetric.character.application.result.GetCharacterSymbolResult;
import java.util.List;

public record GetCharacterSymbolResponse(
        List<SymbolResponse> arcaneSymbols,
        List<SymbolResponse> authenticSymbols
) {

    public static GetCharacterSymbolResponse from(
            GetCharacterSymbolResult result
    ) {
        return new GetCharacterSymbolResponse(
                convert(result.arcaneSymbols()),
                convert(result.authenticSymbols())
        );
    }

    private static List<SymbolResponse> convert(
            List<GetCharacterSymbolResult.SymbolResult> symbols
    ) {
        if (symbols == null) {
            return List.of();
        }

        return symbols.stream()
                .map(symbol -> SymbolResponse.from(symbol))
                .toList();
    }

    public record SymbolResponse(
            String symbolName,
            Integer symbolLevel,
            String symbolIcon,
            String symbolDescription,
            String symbolOtherEffectDescription,
            String symbolForce,
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

        public static SymbolResponse from(
                GetCharacterSymbolResult.SymbolResult symbol
        ) {
            return new SymbolResponse(
                    symbol.symbolName(),
                    symbol.symbolLevel(),
                    symbol.symbolIcon(),
                    symbol.symbolDescription(),
                    symbol.symbolOtherEffectDescription(),
                    symbol.symbolForce(),
                    symbol.symbolStr(),
                    symbol.symbolDex(),
                    symbol.symbolIntelligence(),
                    symbol.symbolLuk(),
                    symbol.symbolHp(),
                    symbol.symbolDropRate(),
                    symbol.symbolMesoRate(),
                    symbol.symbolExpRate(),
                    symbol.symbolGrowthCount(),
                    symbol.symbolRequireGrowthCount()
            );
        }
    }
}
