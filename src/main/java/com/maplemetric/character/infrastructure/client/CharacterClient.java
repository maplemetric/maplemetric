package com.maplemetric.character.infrastructure.client;

import com.maplemetric.character.infrastructure.client.dto.CharacterAbilityResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterBasicResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterDojangResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterEquipmentResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterHexaMatrixResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterHexaMatrixStatResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterHyperStatResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterLinkSkillResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterPopularityResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterSkillResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterSymbolResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterStatResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterUnionResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterVMatrixResponse;

public interface CharacterClient {

    String getOcid(String characterName);

    CharacterBasicResponse getCharacterBasic(String ocid);

    CharacterEquipmentResponse getCharacterEquipment(String ocid);

    CharacterStatResponse getCharacterStat(String ocid);

    CharacterPopularityResponse getCharacterPopularity(String ocid);

    CharacterHyperStatResponse getCharacterHyperStat(String ocid);

    CharacterAbilityResponse getCharacterAbility(String ocid);

    CharacterUnionResponse getCharacterUnion(String ocid);

    CharacterSymbolResponse getCharacterSymbol(String ocid);

    CharacterSkillResponse getCharacterSkill(
            String ocid,
            String skillGrade
    );

    CharacterLinkSkillResponse getCharacterLinkSkill(String ocid);

    CharacterVMatrixResponse getCharacterVMatrix(String ocid);

    CharacterHexaMatrixResponse getCharacterHexaMatrix(String ocid);

    CharacterHexaMatrixStatResponse getCharacterHexaMatrixStat(String ocid);

    CharacterDojangResponse getCharacterDojang(String ocid);
}
