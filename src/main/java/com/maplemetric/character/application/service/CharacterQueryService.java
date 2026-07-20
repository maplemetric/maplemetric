package com.maplemetric.character.application.service;

import com.maplemetric.character.application.result.GetCharacterBasicResult;
import com.maplemetric.character.application.result.GetCharacterEquipmentResult;
import com.maplemetric.character.application.result.GetCharacterStatResult;
import com.maplemetric.character.application.result.GetCharacterSummaryResult;
import com.maplemetric.character.infrastructure.client.CharacterClient;
import com.maplemetric.character.infrastructure.client.dto.CharacterBasicResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterEquipmentResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterStatResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CharacterQueryService {

    private final CharacterClient characterClient;

    public CharacterQueryService(
            CharacterClient characterClient
    ) {
        this.characterClient = characterClient;
    }

    public GetCharacterBasicResult getCharacterBasic(
            String characterName
    ) {
        String ocid = characterClient.getOcid(characterName);

        CharacterBasicResponse basicResponse =
                characterClient.getCharacterBasic(ocid);

        return GetCharacterBasicResult.from(basicResponse);
    }

    public GetCharacterEquipmentResult getCharacterEquipment(
            String characterName
    ) {
        String ocid = characterClient.getOcid(characterName);

        CharacterEquipmentResponse equipmentResponse =
                characterClient.getCharacterEquipment(ocid);

        return GetCharacterEquipmentResult.from(equipmentResponse);
    }

    public GetCharacterSummaryResult getCharacterSummary(
            String characterName
    ) {
        String ocid = characterClient.getOcid(characterName);

        CharacterBasicResponse basicResponse =
                characterClient.getCharacterBasic(ocid);

        CharacterStatResponse statResponse =
                characterClient.getCharacterStat(ocid);

        CharacterEquipmentResponse equipmentResponse =
                characterClient.getCharacterEquipment(ocid);

        return GetCharacterSummaryResult.of(
                GetCharacterBasicResult.from(basicResponse),
                GetCharacterStatResult.from(statResponse),
                GetCharacterEquipmentResult.from(equipmentResponse)
        );
    }
}