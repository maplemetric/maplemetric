package com.maplemetric.character.infrastructure.client.nexon;

import com.maplemetric.character.application.port.out.LoadCharacterDojangPort;
import com.maplemetric.character.application.port.out.LoadCharacterDojangPort.CharacterDojang;
import com.maplemetric.character.infrastructure.client.CharacterClient;
import com.maplemetric.character.infrastructure.client.dto.CharacterDojangResponse;
import org.springframework.stereotype.Component;

@Component
class NexonCharacterDojangAdapter
        implements LoadCharacterDojangPort {

    private final CharacterClient characterClient;

    NexonCharacterDojangAdapter(
            CharacterClient characterClient
    ) {
        this.characterClient = characterClient;
    }

    @Override
    public CharacterDojang loadCharacterDojang(
            String ocid
    ) {
        CharacterDojangResponse response =
                characterClient.getCharacterDojang(ocid);

        return new CharacterDojang(
                response.date(),
                response.characterClass(),
                response.worldName(),
                response.dojangBestFloor(),
                response.dateDojangRecord(),
                response.dojangBestTime()
        );
    }
}
