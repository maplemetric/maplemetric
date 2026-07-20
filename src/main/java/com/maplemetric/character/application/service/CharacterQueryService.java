package com.maplemetric.character.application.service;

import com.maplemetric.character.application.result.GetCharacterBasicResult;
import com.maplemetric.character.application.result.GetCharacterEquipmentResult;
import com.maplemetric.character.application.result.GetCharacterHexaResult;
import com.maplemetric.character.application.result.GetCharacterRankingResult;
import com.maplemetric.character.application.result.GetCharacterSkillsResult;
import com.maplemetric.character.application.result.GetCharacterStatResult;
import com.maplemetric.character.application.result.GetCharacterSummaryResult;
import com.maplemetric.character.application.result.GetCharacterSymbolResult;
import com.maplemetric.character.application.result.GetCharacterUnionResult;
import com.maplemetric.character.domain.exception.CharacterErrorCode;
import com.maplemetric.character.domain.exception.CharacterException;
import com.maplemetric.character.infrastructure.client.CharacterClient;
import com.maplemetric.character.infrastructure.client.dto.CharacterBasicResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterDojangResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterEquipmentResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterHexaMatrixResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterHexaMatrixStatResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterLinkSkillResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterRankingResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterSkillResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterStatResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterSymbolResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterUnionResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterVMatrixResponse;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
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

    private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");

    private static final LocalTime RANKING_AVAILABLE_TIME = LocalTime.of(9, 30);

    private final CharacterClient characterClient;
    private final Clock clock;

    @Autowired
    public CharacterQueryService(
            CharacterClient characterClient
    ) {
        this(
                characterClient,
                Clock.system(KOREA_ZONE_ID)
        );
    }

    CharacterQueryService(
            CharacterClient characterClient,
            Clock clock
    ) {
        this.characterClient = characterClient;
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

        LocalDate rankingDate =
                resolveRankingDate();

        CharacterRankingResponse overallRankingResponse =
                characterClient.getOverallRanking(
                        ocid,
                        rankingDate
                );

        CharacterRankingResponse worldRankingResponse =
                characterClient.getWorldRanking(
                        ocid,
                        basicResponse.worldName(),
                        rankingDate
                );

        String classRankingFilter =
                resolveClassRankingFilter(
                        basicResponse.characterName(),
                        overallRankingResponse
                );

        CharacterRankingResponse classRankingResponse =
                new CharacterRankingResponse(List.of());

        CharacterRankingResponse worldClassRankingResponse =
                new CharacterRankingResponse(List.of());

        if (StringUtils.hasText(classRankingFilter)) {
            classRankingResponse =
                    characterClient.getClassRanking(
                            ocid,
                            classRankingFilter,
                            rankingDate
                    );

            worldClassRankingResponse =
                    characterClient.getWorldClassRanking(
                            ocid,
                            basicResponse.worldName(),
                            classRankingFilter,
                            rankingDate
                    );
        }

        CharacterDojangResponse dojangResponse =
                characterClient.getCharacterDojang(ocid);

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
                        basicResponse.characterName(),
                        overallRankingResponse,
                        worldRankingResponse,
                        classRankingResponse,
                        worldClassRankingResponse,
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
                        equipmentResponse
                )
        );
    }

    private LocalDate resolveRankingDate() {
        ZonedDateTime now =
                ZonedDateTime.now(clock)
                        .withZoneSameInstant(KOREA_ZONE_ID);

        LocalDate today = now.toLocalDate();

        if (now.toLocalTime()
                .isBefore(RANKING_AVAILABLE_TIME)) {
            return today.minusDays(1);
        }

        return today;
    }

    private String resolveClassRankingFilter(
            String characterName,
            CharacterRankingResponse response
    ) {
        if (response == null
                || response.ranking() == null) {
            return null;
        }

        return response.ranking()
                .stream()
                .filter(ranking -> ranking != null)
                .filter(ranking -> Objects.equals(
                        characterName,
                        ranking.characterName()
                ))
                .findFirst()
                .map(ranking -> createClassRankingFilter(
                        ranking.className(),
                        ranking.subClassName()
                ))
                .orElse(null);
    }

    private String createClassRankingFilter(
            String className,
            String subClassName
    ) {
        if (!StringUtils.hasText(className)) {
            return null;
        }

        if (StringUtils.hasText(subClassName)) {
            return className
                    + "-"
                    + subClassName;
        }

        return className
                + "-전체 전직";
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
