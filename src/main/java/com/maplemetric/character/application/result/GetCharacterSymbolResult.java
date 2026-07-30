package com.maplemetric.character.application.result;

import com.maplemetric.character.application.port.out.LoadCharacterSymbolPort.CharacterSymbol;
import com.maplemetric.character.application.port.out.LoadCharacterSymbolPort.Symbol;
import java.util.List;

public record GetCharacterSymbolResult(
        List<SymbolResult> arcaneSymbols,
        List<SymbolResult> authenticSymbols
) {

    private static final String ARCANE_SYMBOL_KEYWORD = "아케인심볼";
    private static final String AUTHENTIC_SYMBOL_KEYWORD = "어센틱심볼";

    public static GetCharacterSymbolResult from(
            CharacterSymbol characterSymbol
    ) {
        List<Symbol> symbols =
                characterSymbol.symbols() == null
                        ? List.of()
                        : characterSymbol.symbols();

        return new GetCharacterSymbolResult(
                convertArcaneSymbols(symbols),
                convertAuthenticSymbols(symbols)
        );
    }

    private static List<SymbolResult> convertArcaneSymbols(
            List<Symbol> symbols
    ) {
        return symbols.stream()
                .filter(symbol -> isArcaneSymbol(symbol))
                .map(symbol -> SymbolResult.from(symbol))
                .toList();
    }

    private static List<SymbolResult> convertAuthenticSymbols(
            List<Symbol> symbols
    ) {
        return symbols.stream()
                .filter(symbol -> isAuthenticSymbol(symbol))
                .map(symbol -> SymbolResult.from(symbol))
                .toList();
    }

    private static boolean isArcaneSymbol(
            Symbol symbol
    ) {
        return symbol.symbolName() != null
                && symbol.symbolName().contains(ARCANE_SYMBOL_KEYWORD);
    }

    private static boolean isAuthenticSymbol(
            Symbol symbol
    ) {
        return symbol.symbolName() != null
                && symbol.symbolName().contains(AUTHENTIC_SYMBOL_KEYWORD);
    }

    public record SymbolResult(
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

        public static SymbolResult from(
                Symbol symbol
        ) {
            return new SymbolResult(
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
