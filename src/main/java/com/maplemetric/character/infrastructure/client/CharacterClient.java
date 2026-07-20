package com.maplemetric.character.infrastructure.client;

import com.maplemetric.character.infrastructure.client.dto.CharacterBasicResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterEquipmentResponse;

public interface CharacterClient {

    String getOcid(String characterName);

    CharacterBasicResponse getCharacterBasic(String ocid);

    CharacterEquipmentResponse getCharacterEquipment(String ocid);
}
