package com.maplemetric.character.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CharacterSymbolResponse(
        String date,
        String characterClass,
        List<Symbol> symbol
) {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Symbol(
            String symbolName,
            String symbolIcon,
            String symbolDescription,
            String symbolOtherEffectDescription,
            String symbolForce,
            Integer symbolLevel,
            String symbolStr,
            String symbolDex,

            // int는 Java 예약어라 필드명을 바꾸고 원본 이름을 직접 지정한다.
            @JsonProperty("symbol_int")
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
