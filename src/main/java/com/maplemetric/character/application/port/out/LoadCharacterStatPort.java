package com.maplemetric.character.application.port.out;

import java.util.List;

public interface LoadCharacterStatPort {

    CharacterStat loadCharacterStat(String ocid);

    record CharacterStat(
            String date,
            String characterClass,
            List<FinalStat> finalStat,
            Integer remainAp
    ) {
    }

    record FinalStat(
            String statName,
            String statValue
    ) {
    }
}
