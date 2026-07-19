package com.maplemetric.character.application.service;

import com.maplemetric.character.application.result.GetCharacterResult;
import com.maplemetric.character.infrastructure.client.CharacterClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CharacterQueryService {

    private final CharacterClient characterClient;

    public CharacterQueryService(CharacterClient characterClient) {
        this.characterClient = characterClient;
    }

    public GetCharacterResult getCharacter(String characterName) {
        String ocid = characterClient.getOcid(characterName);

        return GetCharacterResult.of(characterName, ocid);
    }


}