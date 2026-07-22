package com.maplemetric.character.infrastructure.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maplemetric.character.domain.exception.CharacterErrorCode;
import com.maplemetric.character.domain.exception.CharacterException;
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
import com.maplemetric.character.infrastructure.client.dto.OcidResponse;
import com.maplemetric.common.nexon.NexonApiFailure;
import com.maplemetric.common.nexon.NexonApiRequester;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class CharacterClientImpl implements CharacterClient {

    private static final String CHARACTER_OCID_PATH = "/maplestory/v1/id";

    private static final String CHARACTER_BASIC_PATH = "/maplestory/v1/character/basic";

    private static final String CHARACTER_EQUIPMENT_PATH = "/maplestory/v1/character/item-equipment";

    private static final String CHARACTER_STAT_PATH = "/maplestory/v1/character/stat";

    private static final String CHARACTER_POPULARITY_PATH = "/maplestory/v1/character/popularity";

    private static final String CHARACTER_HYPER_STAT_PATH = "/maplestory/v1/character/hyper-stat";

    private static final String CHARACTER_ABILITY_PATH = "/maplestory/v1/character/ability";

    private static final String CHARACTER_UNION_PATH = "/maplestory/v1/user/union";

    private static final String CHARACTER_SYMBOL_PATH = "/maplestory/v1/character/symbol-equipment";

    private static final String CHARACTER_SKILL_PATH = "/maplestory/v1/character/skill";

    private static final String CHARACTER_LINK_SKILL_PATH = "/maplestory/v1/character/link-skill";

    private static final String CHARACTER_V_MATRIX_PATH = "/maplestory/v1/character/vmatrix";

    private static final String CHARACTER_HEXA_MATRIX_PATH = "/maplestory/v1/character/hexamatrix";

    private static final String CHARACTER_HEXA_MATRIX_STAT_PATH = "/maplestory/v1/character/hexamatrix-stat";

    private static final String CHARACTER_DOJANG_PATH = "/maplestory/v1/character/dojang";

    private static final String CHARACTER_OCID_API = "캐릭터 식별자";

    private static final String CHARACTER_BASIC_API = "캐릭터 기본 정보";

    private static final String CHARACTER_EQUIPMENT_API = "캐릭터 장비 정보";

    private static final String CHARACTER_STAT_API = "캐릭터 스탯 정보";

    private static final String CHARACTER_UNION_API = "캐릭터 유니온 정보";

    private static final String CHARACTER_SYMBOL_API = "캐릭터 장착 심볼 정보";

    private static final String CHARACTER_LINK_SKILL_API = "캐릭터 링크 스킬 정보";

    private static final String CHARACTER_V_MATRIX_API = "캐릭터 V매트릭스 정보";

    private static final String CHARACTER_HEXA_MATRIX_API = "캐릭터 HEXA 코어 정보";

    private static final String CHARACTER_HEXA_MATRIX_STAT_API = "캐릭터 HEXA 스탯 정보";

    private static final String CHARACTER_DOJANG_API = "캐릭터 무릉도장 정보";

    private static final String CHARACTER_POPULARITY_API = "캐릭터 인기도 정보";

    private static final String CHARACTER_HYPER_STAT_API = "캐릭터 하이퍼스탯 정보";

    private static final String CHARACTER_ABILITY_API = "캐릭터 어빌리티 정보";

    private static final String INVALID_IDENTIFIER_ERROR_CODE = "OPENAPI00003";

    private static final String INVALID_PARAMETER_ERROR_CODE = "OPENAPI00004";

    private final NexonApiRequester nexonApiRequester;

    public CharacterClientImpl(
            @Qualifier("nexonRestClient") RestClient nexonRestClient,
            ObjectMapper objectMapper
    ) {
        this.nexonApiRequester = new NexonApiRequester(
                nexonRestClient,
                objectMapper,
                this::createException,
                this::isCharacterNotFound
        );
    }

    @Override
    public String getOcid(
            String characterName
    ) {
        OcidResponse response = request(
                CHARACTER_OCID_PATH,
                "character_name",
                characterName,
                OcidResponse.class,
                CHARACTER_OCID_API,
                "characterName",
                characterName
        );

        if (!StringUtils.hasText(response.ocid())) {
            log.warn(
                    "넥슨 API OCID 응답이 올바르지 않습니다. characterName={}",
                    characterName
            );

            throw new CharacterException(
                    CharacterErrorCode.NEXON_API_RESPONSE_INVALID
            );
        }

        return response.ocid();
    }

    @Override
    public CharacterBasicResponse getCharacterBasic(
            String ocid
    ) {
        return request(
                CHARACTER_BASIC_PATH,
                "ocid",
                ocid,
                CharacterBasicResponse.class,
                CHARACTER_BASIC_API,
                "ocid",
                ocid
        );
    }

    @Override
    public CharacterEquipmentResponse getCharacterEquipment(
            String ocid
    ) {
        return request(
                CHARACTER_EQUIPMENT_PATH,
                "ocid",
                ocid,
                CharacterEquipmentResponse.class,
                CHARACTER_EQUIPMENT_API,
                "ocid",
                ocid
        );
    }

    @Override
    public CharacterStatResponse getCharacterStat(
            String ocid
    ) {
        return request(
                CHARACTER_STAT_PATH,
                "ocid",
                ocid,
                CharacterStatResponse.class,
                CHARACTER_STAT_API,
                "ocid",
                ocid
        );
    }

    @Override
    public CharacterPopularityResponse getCharacterPopularity(
            String ocid
    ) {
        return request(
                CHARACTER_POPULARITY_PATH,
                "ocid",
                ocid,
                CharacterPopularityResponse.class,
                CHARACTER_POPULARITY_API,
                "ocid",
                ocid
        );
    }

    @Override
    public CharacterHyperStatResponse getCharacterHyperStat(
            String ocid
    ) {
        return request(
                CHARACTER_HYPER_STAT_PATH,
                "ocid",
                ocid,
                CharacterHyperStatResponse.class,
                CHARACTER_HYPER_STAT_API,
                "ocid",
                ocid
        );
    }

    @Override
    public CharacterAbilityResponse getCharacterAbility(
            String ocid
    ) {
        return request(
                CHARACTER_ABILITY_PATH,
                "ocid",
                ocid,
                CharacterAbilityResponse.class,
                CHARACTER_ABILITY_API,
                "ocid",
                ocid
        );
    }

    @Override
    public CharacterUnionResponse getCharacterUnion(
            String ocid
    ) {
        return request(
                CHARACTER_UNION_PATH,
                "ocid",
                ocid,
                CharacterUnionResponse.class,
                CHARACTER_UNION_API,
                "ocid",
                ocid
        );
    }

    @Override
    public CharacterSymbolResponse getCharacterSymbol(
            String ocid
    ) {
        return request(
                CHARACTER_SYMBOL_PATH,
                "ocid",
                ocid,
                CharacterSymbolResponse.class,
                CHARACTER_SYMBOL_API,
                "ocid",
                ocid
        );
    }

    @Override
    public CharacterSkillResponse getCharacterSkill(
            String ocid,
            String skillGrade
    ) {
        LinkedHashMap<String, String> queryParameters =
                new LinkedHashMap<>();

        queryParameters.put("ocid", ocid);
        queryParameters.put(
                "character_skill_grade",
                skillGrade
        );

        return request(
                CHARACTER_SKILL_PATH,
                queryParameters,
                CharacterSkillResponse.class,
                "캐릭터 " + skillGrade + "차 스킬 정보",
                "ocid",
                ocid
        );
    }

    @Override
    public CharacterLinkSkillResponse getCharacterLinkSkill(
            String ocid
    ) {
        return request(
                CHARACTER_LINK_SKILL_PATH,
                "ocid",
                ocid,
                CharacterLinkSkillResponse.class,
                CHARACTER_LINK_SKILL_API,
                "ocid",
                ocid
        );
    }

    @Override
    public CharacterVMatrixResponse getCharacterVMatrix(
            String ocid
    ) {
        return request(
                CHARACTER_V_MATRIX_PATH,
                "ocid",
                ocid,
                CharacterVMatrixResponse.class,
                CHARACTER_V_MATRIX_API,
                "ocid",
                ocid
        );
    }

    @Override
    public CharacterHexaMatrixResponse getCharacterHexaMatrix(
            String ocid
    ) {
        return request(
                CHARACTER_HEXA_MATRIX_PATH,
                "ocid",
                ocid,
                CharacterHexaMatrixResponse.class,
                CHARACTER_HEXA_MATRIX_API,
                "ocid",
                ocid
        );
    }

    @Override
    public CharacterHexaMatrixStatResponse getCharacterHexaMatrixStat(
            String ocid
    ) {
        return request(
                CHARACTER_HEXA_MATRIX_STAT_PATH,
                "ocid",
                ocid,
                CharacterHexaMatrixStatResponse.class,
                CHARACTER_HEXA_MATRIX_STAT_API,
                "ocid",
                ocid
        );
    }

    @Override
    public CharacterDojangResponse getCharacterDojang(
            String ocid
    ) {
        return request(
                CHARACTER_DOJANG_PATH,
                "ocid",
                ocid,
                CharacterDojangResponse.class,
                CHARACTER_DOJANG_API,
                "ocid",
                ocid
        );
    }

    private <T> T request(
            String path,
            String queryParameterName,
            String queryParameterValue,
            Class<T> responseType,
            String apiName,
            String identifierName,
            String identifierValue
    ) {
        return request(
                path,
                Map.of(queryParameterName, queryParameterValue),
                responseType,
                apiName,
                identifierName,
                identifierValue
        );
    }

    private <T> T request(
            String path,
            Map<String, String> queryParameters,
            Class<T> responseType,
            String apiName,
            String identifierName,
            String identifierValue
    ) {
        return nexonApiRequester.request(
                path,
                queryParameters,
                responseType,
                apiName,
                identifierName,
                identifierValue
        );
    }

    private CharacterException createException(
            NexonApiFailure failure
    ) {
        CharacterErrorCode errorCode = switch (failure) {
            case NOT_FOUND ->
                    CharacterErrorCode.CHARACTER_NOT_FOUND;
            case CLIENT_ERROR ->
                    CharacterErrorCode.NEXON_API_CLIENT_ERROR;
            case SERVER_ERROR ->
                    CharacterErrorCode.NEXON_API_SERVER_ERROR;
            case TIMEOUT ->
                    CharacterErrorCode.NEXON_API_TIMEOUT;
            case RESPONSE_INVALID ->
                    CharacterErrorCode.NEXON_API_RESPONSE_INVALID;
        };

        return new CharacterException(errorCode);
    }

    private boolean isCharacterNotFound(
            String apiName,
            String nexonErrorCode
    ) {
        if (!CHARACTER_OCID_API.equals(apiName)) {
            return false;
        }

        return INVALID_IDENTIFIER_ERROR_CODE.equals(
                nexonErrorCode
        ) || INVALID_PARAMETER_ERROR_CODE.equals(
                nexonErrorCode
        );
    }
}
