package com.maplemetric.character.infrastructure.client;

import com.maplemetric.character.infrastructure.client.dto.CharacterBasicResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterEquipmentResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterSymbolResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterStatResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterUnionResponse;

public interface CharacterClient {

    String getOcid(String characterName);

    CharacterBasicResponse getCharacterBasic(String ocid);

    CharacterEquipmentResponse getCharacterEquipment(String ocid);

    CharacterStatResponse getCharacterStat(String ocid);

    CharacterUnionResponse getCharacterUnion(String ocid);

    CharacterSymbolResponse getCharacterSymbol(String ocid);
}
