package com.maplemetric.character.application.port.out;

import java.util.List;

public interface LoadCharacterHyperStatPort {

    CharacterHyperStat loadCharacterHyperStat(String ocid);

    record CharacterHyperStat(
            String date,
            String characterClass,
            String appliedPresetNo,
            Long availablePoints,
            List<HyperStat> preset1,
            Long preset1RemainPoints,
            List<HyperStat> preset2,
            Long preset2RemainPoints,
            List<HyperStat> preset3,
            Long preset3RemainPoints
    ) {
    }

    record HyperStat(
            String statType,
            Long statPoint,
            Integer statLevel,
            String statIncrease
    ) {
    }
}
