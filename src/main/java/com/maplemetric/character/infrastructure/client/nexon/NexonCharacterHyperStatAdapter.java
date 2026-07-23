package com.maplemetric.character.infrastructure.client.nexon;

import com.maplemetric.character.application.port.out.LoadCharacterHyperStatPort;
import com.maplemetric.character.application.port.out.LoadCharacterHyperStatPort.CharacterHyperStat;
import com.maplemetric.character.infrastructure.client.CharacterClient;
import com.maplemetric.character.infrastructure.client.dto.CharacterHyperStatResponse;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
class NexonCharacterHyperStatAdapter
        implements LoadCharacterHyperStatPort {

    private final CharacterClient characterClient;

    NexonCharacterHyperStatAdapter(
            CharacterClient characterClient
    ) {
        this.characterClient = characterClient;
    }

    @Override
    public CharacterHyperStat loadCharacterHyperStat(
            String ocid
    ) {
        CharacterHyperStatResponse response =
                characterClient.getCharacterHyperStat(ocid);

        return new CharacterHyperStat(
                response.date(),
                response.characterClass(),
                response.usePresetNo(),
                response.useAvailableHyperStat(),
                convertStats(response.hyperStatPreset1()),
                response.hyperStatPreset1RemainPoint(),
                convertStats(response.hyperStatPreset2()),
                response.hyperStatPreset2RemainPoint(),
                convertStats(response.hyperStatPreset3()),
                response.hyperStatPreset3RemainPoint()
        );
    }

    private List<LoadCharacterHyperStatPort.HyperStat> convertStats(
            List<CharacterHyperStatResponse.HyperStat> stats
    ) {
        if (stats == null) {
            return null;
        }

        return stats.stream()
                .map(stat -> stat == null
                        ? null
                        : new LoadCharacterHyperStatPort.HyperStat(
                                stat.statType(),
                                stat.statPoint(),
                                stat.statLevel(),
                                stat.statIncrease()
                        ))
                .toList();
    }
}
