package com.maplemetric.character.infrastructure.client.nexon;

import com.maplemetric.character.application.port.out.LoadCharacterBasicPort;
import com.maplemetric.character.application.port.out.LoadCharacterBasicPort.CharacterBasic;
import com.maplemetric.character.infrastructure.client.CharacterClient;
import com.maplemetric.character.infrastructure.client.dto.CharacterBasicResponse;
import org.springframework.stereotype.Component;

@Component
class NexonCharacterBasicAdapter
        implements LoadCharacterBasicPort {

    private final CharacterClient characterClient;

    NexonCharacterBasicAdapter(
            CharacterClient characterClient
    ) {
        this.characterClient = characterClient;
    }

    @Override
    public String resolveOcid(
            String characterName
    ) {
        return characterClient.getOcid(characterName);
    }

    @Override
    public CharacterBasic loadCharacterBasic(
            String ocid
    ) {
        CharacterBasicResponse response =
                characterClient.getCharacterBasic(ocid);

        return new CharacterBasic(
                response.characterName(),
                response.worldName(),
                response.characterGender(),
                response.characterClass(),
                response.characterClassLevel(),
                response.characterLevel(),
                response.characterExp(),
                response.characterExpRate(),
                response.characterGuildName(),
                response.characterImage(),
                response.characterDateCreate(),
                response.accessFlag(),
                response.liberationQuestClearFlag()
        );
    }
}
