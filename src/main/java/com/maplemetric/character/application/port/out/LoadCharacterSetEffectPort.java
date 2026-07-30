package com.maplemetric.character.application.port.out;

import java.util.List;

public interface LoadCharacterSetEffectPort {

    CharacterSetEffect loadCharacterSetEffect(String ocid);

    record CharacterSetEffect(
            List<SetEffect> setEffects
    ) {
    }

    record SetEffect(
            String setName,
            Integer totalSetCount,
            List<SetOption> appliedOptions,
            List<SetOption> fullOptions
    ) {
    }

    record SetOption(
            Integer setCount,
            String setOption
    ) {
    }
}
