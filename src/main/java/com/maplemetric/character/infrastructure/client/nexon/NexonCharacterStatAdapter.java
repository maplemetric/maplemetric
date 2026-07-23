package com.maplemetric.character.infrastructure.client.nexon;

import com.maplemetric.character.application.port.out.LoadCharacterStatPort;
import com.maplemetric.character.application.port.out.LoadCharacterStatPort.CharacterStat;
import com.maplemetric.character.infrastructure.client.CharacterClient;
import com.maplemetric.character.infrastructure.client.dto.CharacterStatResponse;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
class NexonCharacterStatAdapter
        implements LoadCharacterStatPort {

    private final CharacterClient characterClient;

    NexonCharacterStatAdapter(
            CharacterClient characterClient
    ) {
        this.characterClient = characterClient;
    }

    @Override
    public CharacterStat loadCharacterStat(
            String ocid
    ) {
        CharacterStatResponse response =
                characterClient.getCharacterStat(ocid);

        return new CharacterStat(
                response.date(),
                response.characterClass(),
                convertFinalStats(response),
                response.remainAp()
        );
    }

    private List<LoadCharacterStatPort.FinalStat> convertFinalStats(
            CharacterStatResponse response
    ) {
        if (response.finalStat() == null) {
            return null;
        }

        return response.finalStat().stream()
                .map(finalStat -> new LoadCharacterStatPort.FinalStat(
                        finalStat.statName(),
                        finalStat.statValue()
                ))
                .toList();
    }
}
