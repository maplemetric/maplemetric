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
            String symbolIcon
    ) {

        public static SymbolResponse from(
                GetCharacterSymbolResult.SymbolResult symbol
        ) {
            return new SymbolResponse(
                    symbol.symbolName(),
                    symbol.symbolLevel(),
                    symbol.symbolIcon()
            );
        }
    }
}
