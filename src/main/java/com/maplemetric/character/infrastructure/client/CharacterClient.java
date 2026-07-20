package com.maplemetric.character.infrastructure.client;

import com.maplemetric.character.infrastructure.client.dto.CharacterBasicResponse;

public interface CharacterClient {

    String getOcid(String characterName);

    CharacterBasicResponse getCharacterBasic(String ocid);
}
