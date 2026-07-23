package com.maplemetric.character.infrastructure.client.nexon;

import com.maplemetric.character.application.port.out.LoadCharacterPopularityPort;
import com.maplemetric.character.application.port.out.LoadCharacterPopularityPort.CharacterPopularity;
import com.maplemetric.character.infrastructure.client.CharacterClient;
import com.maplemetric.character.infrastructure.client.dto.CharacterPopularityResponse;
import org.springframework.stereotype.Component;

@Component
class NexonCharacterPopularityAdapter
        implements LoadCharacterPopularityPort {

    private final CharacterClient characterClient;

    NexonCharacterPopularityAdapter(
            CharacterClient characterClient
    ) {
        this.characterClient = characterClient;
    }

    @Override
    public CharacterPopularity loadCharacterPopularity(
            String ocid
    ) {
        CharacterPopularityResponse response =
                characterClient.getCharacterPopularity(ocid);

        return new CharacterPopularity(
                response.date(),
                response.popularity()
        );
    }
}
