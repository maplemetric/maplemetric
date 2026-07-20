package com.maplemetric.character.application.result;

import com.maplemetric.character.infrastructure.client.dto.CharacterSymbolResponse;
import java.util.List;

public record GetCharacterSymbolResult(
        List<SymbolResult> arcaneSymbols,
        List<SymbolResult> authenticSymbols
) {

    private static final String ARCANE_SYMBOL_KEYWORD = "아케인심볼";
    private static final String AUTHENTIC_SYMBOL_KEYWORD = "어센틱심볼";

    public static GetCharacterSymbolResult from(
            CharacterSymbolResponse response
    ) {
        List<CharacterSymbolResponse.Symbol> symbols =
                response.symbol() == null
                        ? List.of()
                        : response.symbol();

        return new GetCharacterSymbolResult(
                convertArcaneSymbols(symbols),
                convertAuthenticSymbols(symbols)
        );
    }

    private static List<SymbolResult> convertArcaneSymbols(
            List<CharacterSymbolResponse.Symbol> symbols
    ) {
        return symbols.stream()
                .filter(symbol -> isArcaneSymbol(symbol))
                .map(symbol -> SymbolResult.from(symbol))
                .toList();
    }

    private static List<SymbolResult> convertAuthenticSymbols(
            List<CharacterSymbolResponse.Symbol> symbols
    ) {
        return symbols.stream()
                .filter(symbol -> isAuthenticSymbol(symbol))
                .map(symbol -> SymbolResult.from(symbol))
                .toList();
    }

    private static boolean isArcaneSymbol(
            CharacterSymbolResponse.Symbol symbol
    ) {
        return symbol.symbolName() != null
                && symbol.symbolName().contains(ARCANE_SYMBOL_KEYWORD);
    }

    private static boolean isAuthenticSymbol(
            CharacterSymbolResponse.Symbol symbol
    ) {
        return symbol.symbolName() != null
                && symbol.symbolName().contains(AUTHENTIC_SYMBOL_KEYWORD);
    }

    public record SymbolResult(
            String symbolName,
            Integer symbolLevel,
            String symbolIcon
    ) {

        public static SymbolResult from(
                CharacterSymbolResponse.Symbol symbol
        ) {
            return new SymbolResult(
                    symbol.symbolName(),
                    symbol.symbolLevel(),
                    symbol.symbolIcon()
            );
        }
    }
}
