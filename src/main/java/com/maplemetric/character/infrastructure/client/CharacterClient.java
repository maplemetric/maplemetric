package com.maplemetric.character.infrastructure.client;

import com.maplemetric.character.infrastructure.client.dto.CharacterBasicResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterDojangResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterEquipmentResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterHexaMatrixResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterHexaMatrixStatResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterLinkSkillResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterRankingResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterSkillResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterSymbolResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterStatResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterUnionResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterVMatrixResponse;
import java.time.LocalDate;

public interface CharacterClient {

    String getOcid(String characterName);

    CharacterBasicResponse getCharacterBasic(String ocid);

    CharacterEquipmentResponse getCharacterEquipment(String ocid);

    CharacterStatResponse getCharacterStat(String ocid);

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

    CharacterRankingResponse getOverallRanking(String ocid, LocalDate date);

    CharacterRankingResponse getWorldRanking(
            String ocid,
            String worldName,
            LocalDate date
    );

    CharacterRankingResponse getClassRanking(
            String ocid,
            String classRankingFilter,
            LocalDate date
    );

    CharacterRankingResponse getWorldClassRanking(
            String ocid,
            String worldName,
            String classRankingFilter,
            LocalDate date
    );

    CharacterDojangResponse getCharacterDojang(String ocid);
}
