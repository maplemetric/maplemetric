package com.maplemetric.character.application.service;

import com.maplemetric.character.application.calculator.AdditionalOptionCalculator;
import com.maplemetric.character.application.result.GetCharacterAbilityResult;
import com.maplemetric.character.application.result.GetCharacterBasicResult;
import com.maplemetric.character.application.result.GetCharacterDojangResult;
import com.maplemetric.character.application.result.GetCharacterEquipmentResult;
import com.maplemetric.character.application.result.GetCharacterHexaResult;
import com.maplemetric.character.application.result.GetCharacterHyperStatResult;
import com.maplemetric.character.application.result.GetCharacterPopularityResult;
import com.maplemetric.character.application.result.GetCharacterRankingResult;
import com.maplemetric.character.application.result.GetCharacterSkillsResult;
import com.maplemetric.character.application.result.GetCharacterStatResult;
import com.maplemetric.character.application.result.GetCharacterSummaryResult;
import com.maplemetric.character.application.result.GetCharacterSymbolResult;
import com.maplemetric.character.application.result.GetCharacterUnionResult;
import com.maplemetric.character.domain.exception.CharacterErrorCode;
import com.maplemetric.character.domain.exception.CharacterException;
import com.maplemetric.character.infrastructure.client.CharacterClient;
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
import com.maplemetric.character.infrastructure.client.dto.CharacterStatResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterSymbolResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterUnionResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterVMatrixResponse;
import com.maplemetric.ranking.CharacterRanking;
import com.maplemetric.ranking.CharacterRankingQuery;
import com.maplemetric.ranking.CharacterRankingQueryException;
import java.time.Clock;
import java.time.Instant;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CharacterQueryService {

    private static final Pattern CHARACTER_NAME_PATTERN = Pattern.compile("^[가-힣A-Za-z0-9]+$");

    private static final int MIN_CHARACTER_NAME_LENGTH = 4;
    private static final int MAX_CHARACTER_NAME_LENGTH = 12;

    private static final int KOREAN_CHARACTER_WEIGHT = 2;
    private static final int ENGLISH_NUMBER_CHARACTER_WEIGHT = 1;

    private final CharacterClient characterClient;
    private final AdditionalOptionCalculator additionalOptionCalculator;
    private final CharacterRankingQuery characterRankingQuery;
    private final Clock clock;

    @Autowired
    public CharacterQueryService(
            CharacterClient characterClient,
            AdditionalOptionCalculator additionalOptionCalculator,
            CharacterRankingQuery characterRankingQuery
    ) {
        this(
                characterClient,
                additionalOptionCalculator,
                characterRankingQuery,
                Clock.systemUTC()
        );
    }

    CharacterQueryService(
            CharacterClient characterClient,
            AdditionalOptionCalculator additionalOptionCalculator,
            CharacterRankingQuery characterRankingQuery,
            Clock clock
    ) {
        this.characterClient = characterClient;
        this.additionalOptionCalculator =
                additionalOptionCalculator;
        this.characterRankingQuery = characterRankingQuery;
        this.clock = clock;
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
                equipmentResponse,
                equipmentResponse.characterClass(),
                additionalOptionCalculator
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

        CharacterRanking characterRanking =
                getCharacterRanking(
                        ocid,
                        basicResponse
                );

        CharacterDojangResponse dojangResponse =
                characterClient.getCharacterDojang(ocid);

        CharacterPopularityResponse popularityResponse =
                characterClient.getCharacterPopularity(ocid);

        CharacterHyperStatResponse hyperStatResponse =
                characterClient.getCharacterHyperStat(ocid);

        CharacterAbilityResponse abilityResponse =
                characterClient.getCharacterAbility(ocid);

        CharacterUnionResponse unionResponse =
                characterClient.getCharacterUnion(ocid);

        CharacterSymbolResponse symbolResponse =
                characterClient.getCharacterSymbol(ocid);

        CharacterSkillResponse fifthSkillResponse =
                characterClient.getCharacterSkill(
                        ocid,
                        "5"
                );

        CharacterVMatrixResponse vMatrixResponse =
                characterClient.getCharacterVMatrix(ocid);

        CharacterLinkSkillResponse linkSkillResponse =
                characterClient.getCharacterLinkSkill(ocid);

        CharacterSkillResponse sixthSkillResponse =
                characterClient.getCharacterSkill(
                        ocid,
                        "6"
                );

        CharacterHexaMatrixResponse hexaMatrixResponse =
                characterClient.getCharacterHexaMatrix(ocid);

        CharacterHexaMatrixStatResponse hexaStatResponse =
                characterClient.getCharacterHexaMatrixStat(ocid);

        CharacterEquipmentResponse equipmentResponse =
                characterClient.getCharacterEquipment(ocid);

        return GetCharacterSummaryResult.of(
                GetCharacterBasicResult.from(basicResponse),
                GetCharacterStatResult.from(statResponse),
                GetCharacterRankingResult.of(
                        characterRanking,
                        dojangResponse
                ),
                GetCharacterUnionResult.from(unionResponse),
                GetCharacterSymbolResult.from(symbolResponse),
                GetCharacterSkillsResult.of(
                        vMatrixResponse,
                        fifthSkillResponse,
                        linkSkillResponse
                ),
                GetCharacterHexaResult.of(
                        hexaMatrixResponse,
                        hexaStatResponse,
                        sixthSkillResponse
                ),
                GetCharacterEquipmentResult.from(
                        equipmentResponse,
                        basicResponse.characterClass(),
                        additionalOptionCalculator
                ),
                GetCharacterPopularityResult.from(
                        popularityResponse
                ),
                GetCharacterHyperStatResult.from(
                        hyperStatResponse
                ),
                GetCharacterAbilityResult.from(
                        abilityResponse
                ),
                GetCharacterDojangResult.from(
                        dojangResponse
                ),
                Instant.now(clock).toString()
        );
    }

    private CharacterRanking getCharacterRanking(
            String ocid,
            CharacterBasicResponse basicResponse
    ) {
        try {
            return characterRankingQuery.getCharacterRanking(
                    ocid,
                    basicResponse.characterName(),
                    basicResponse.worldName()
            );
        } catch (CharacterRankingQueryException exception) {
            throw new CharacterException(
                    resolveCharacterErrorCode(
                            exception
                    )
            );
        }
    }

    private CharacterErrorCode resolveCharacterErrorCode(
            CharacterRankingQueryException exception
    ) {
        return switch (exception.getFailure()) {
            case NOT_FOUND, CLIENT_ERROR ->
                    CharacterErrorCode.NEXON_API_CLIENT_ERROR;
            case SERVER_ERROR ->
                    CharacterErrorCode.NEXON_API_SERVER_ERROR;
            case TIMEOUT ->
                    CharacterErrorCode.NEXON_API_TIMEOUT;
            case RESPONSE_INVALID ->
                    CharacterErrorCode.NEXON_API_RESPONSE_INVALID;
        };
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
                calculateCharacterNameLength(
                        characterName
                );

        if (characterNameLength
                < MIN_CHARACTER_NAME_LENGTH
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
                .map(codePoint ->
                        isKoreanCharacter(codePoint)
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
