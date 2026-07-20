package com.maplemetric.character.application.service;

import com.maplemetric.character.application.result.GetCharacterBasicResult;
import com.maplemetric.character.application.result.GetCharacterEquipmentResult;
import com.maplemetric.character.application.result.GetCharacterStatResult;
import com.maplemetric.character.application.result.GetCharacterSummaryResult;
import com.maplemetric.character.domain.exception.CharacterErrorCode;
import com.maplemetric.character.domain.exception.CharacterException;
import com.maplemetric.character.infrastructure.client.CharacterClient;
import com.maplemetric.character.infrastructure.client.dto.CharacterBasicResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterEquipmentResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterStatResponse;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CharacterQueryService {

    private static final Pattern CHARACTER_NAME_PATTERN =
            Pattern.compile("^[가-힣A-Za-z0-9]+$");

    private static final int MIN_CHARACTER_NAME_LENGTH = 4;
    private static final int MAX_CHARACTER_NAME_LENGTH = 12;

    private static final int KOREAN_CHARACTER_WEIGHT = 2;
    private static final int ENGLISH_NUMBER_CHARACTER_WEIGHT = 1;

    private final CharacterClient characterClient;

    public CharacterQueryService(
            CharacterClient characterClient
    ) {
        this.characterClient = characterClient;
    }

    public GetCharacterBasicResult getCharacterBasic(
            String characterName
    ) {
        String ocid = getValidatedOcid(characterName);

        CharacterBasicResponse basicResponse =
                characterClient.getCharacterBasic(ocid);

        return GetCharacterBasicResult.from(basicResponse);
    }

    public GetCharacterEquipmentResult getCharacterEquipment(
            String characterName
    ) {
        String ocid = getValidatedOcid(characterName);

        CharacterEquipmentResponse equipmentResponse =
                characterClient.getCharacterEquipment(ocid);

        return GetCharacterEquipmentResult.from(
                equipmentResponse
        );
    }

    public GetCharacterSummaryResult getCharacterSummary(
            String characterName
    ) {
        String ocid = getValidatedOcid(characterName);

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

    private String getValidatedOcid(
            String characterName
    ) {
        validateCharacterName(characterName);

        return characterClient.getOcid(characterName);
    }

    private void validateCharacterName(
            String characterName
    ) {
        if (!StringUtils.hasText(characterName)
                || !CHARACTER_NAME_PATTERN
                .matcher(characterName)
                .matches()) {
            throw new CharacterException(
                    CharacterErrorCode.INVALID_CHARACTER_NAME
            );
        }

        int characterNameLength =
                calculateCharacterNameLength(characterName);

        if (characterNameLength < MIN_CHARACTER_NAME_LENGTH
                || characterNameLength
                > MAX_CHARACTER_NAME_LENGTH) {
            throw new CharacterException(
                    CharacterErrorCode.INVALID_CHARACTER_NAME
            );
        }
    }

    private int calculateCharacterNameLength(
            String characterName
    ) {
        return characterName.codePoints()
                .map(codePoint -> isKoreanCharacter(codePoint)
                        ? KOREAN_CHARACTER_WEIGHT
                        : ENGLISH_NUMBER_CHARACTER_WEIGHT
                )
                .sum();
    }

    private boolean isKoreanCharacter(
            int codePoint
    ) {
        return codePoint >= '가'
                && codePoint <= '힣';
    }
}