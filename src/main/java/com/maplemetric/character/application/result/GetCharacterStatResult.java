package com.maplemetric.character.application.result;

import com.maplemetric.character.infrastructure.client.dto.CharacterStatResponse;
import com.maplemetric.character.infrastructure.client.dto.FinalStat;

import java.util.List;

public record GetCharacterStatResult(
        String date,
        String characterClass,
        String combatPower,
        Integer remainAp,
        List<FinalStatResult> finalStat
) {

    private static final String COMBAT_POWER_STAT_NAME = "전투력";

    public static GetCharacterStatResult from(CharacterStatResponse response) {
        List<FinalStatResult> finalStatResults =
                response.finalStat() == null
                        ? List.of()
                        : response.finalStat().stream()
                        .map(finalStat -> FinalStatResult.from(finalStat))
                        .toList();

        String combatPower = finalStatResults.stream()
                .filter(finalStat ->
                        COMBAT_POWER_STAT_NAME.equals(finalStat.statName())
                )
                .map(finalStat -> finalStat.statValue())
                .findFirst()
                .orElse(null);

        return new GetCharacterStatResult(
                response.date(),
                response.characterClass(),
                combatPower,
                response.remainAp(),
                finalStatResults
        );
    }

    public record FinalStatResult(
            String statName,
            String statValue
    ) {

        public static FinalStatResult from(FinalStat finalStat) {
            return new FinalStatResult(
                    finalStat.statName(),
                    finalStat.statValue()
            );
        }
    }
}