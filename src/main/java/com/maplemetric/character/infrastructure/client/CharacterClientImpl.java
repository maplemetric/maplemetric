package com.maplemetric.character.infrastructure.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maplemetric.character.domain.exception.CharacterErrorCode;
import com.maplemetric.character.domain.exception.CharacterException;
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
import com.maplemetric.character.infrastructure.client.dto.NexonApiErrorResponse;
import com.maplemetric.character.infrastructure.client.dto.OcidResponse;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
public class CharacterClientImpl implements CharacterClient {

    private static final String CHARACTER_OCID_PATH = "/maplestory/v1/id";

    private static final String CHARACTER_BASIC_PATH = "/maplestory/v1/character/basic";

    private static final String CHARACTER_EQUIPMENT_PATH = "/maplestory/v1/character/item-equipment";

    private static final String CHARACTER_STAT_PATH = "/maplestory/v1/character/stat";

    private static final String CHARACTER_UNION_PATH = "/maplestory/v1/user/union";

    private static final String CHARACTER_SYMBOL_PATH = "/maplestory/v1/character/symbol-equipment";

    private static final String CHARACTER_SKILL_PATH = "/maplestory/v1/character/skill";

    private static final String CHARACTER_LINK_SKILL_PATH = "/maplestory/v1/character/link-skill";

    private static final String CHARACTER_V_MATRIX_PATH = "/maplestory/v1/character/vmatrix";

    private static final String CHARACTER_HEXA_MATRIX_PATH = "/maplestory/v1/character/hexamatrix";

    private static final String CHARACTER_HEXA_MATRIX_STAT_PATH = "/maplestory/v1/character/hexamatrix-stat";

    private static final String RANKING_OVERALL_PATH = "/maplestory/v1/ranking/overall";

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

    private static final String CHARACTER_OVERALL_RANKING_API = "캐릭터 종합 랭킹 정보";

    private static final String CHARACTER_WORLD_RANKING_API = "캐릭터 월드 랭킹 정보";

    private static final String CHARACTER_WORLD_CLASS_RANKING_API = "캐릭터 월드 내 직업 랭킹 정보";

    private static final String CHARACTER_CLASS_RANKING_API = "캐릭터 직업 랭킹 정보";

    private static final String CHARACTER_DOJANG_API = "캐릭터 무릉도장 정보";

    private static final String INVALID_IDENTIFIER_ERROR_CODE = "OPENAPI00003";

    private static final String INVALID_PARAMETER_ERROR_CODE = "OPENAPI00004";

    private static final String RATE_LIMIT_ERROR_CODE = "OPENAPI00007";

    private static final int MAX_RATE_LIMIT_RETRY_COUNT = 3;

    private static final long RATE_LIMIT_RETRY_DELAY_MILLIS = 1_000L;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public CharacterClientImpl(
            @Qualifier("nexonRestClient") RestClient nexonRestClient,
            ObjectMapper objectMapper
    ) {
        this.restClient = nexonRestClient;
        this.objectMapper = objectMapper;
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
    public CharacterRankingResponse getOverallRanking(
            String ocid,
            LocalDate date
    ) {
        LinkedHashMap<String, String> queryParameters =
                new LinkedHashMap<>();

        queryParameters.put("date", date.toString());
        queryParameters.put("ocid", ocid);

        return request(
                RANKING_OVERALL_PATH,
                queryParameters,
                CharacterRankingResponse.class,
                CHARACTER_OVERALL_RANKING_API,
                "ocid",
                ocid
        );
    }

    @Override
    public CharacterRankingResponse getWorldRanking(
            String ocid,
            String worldName,
            LocalDate date
    ) {
        LinkedHashMap<String, String> queryParameters =
                new LinkedHashMap<>();

        queryParameters.put("date", date.toString());
        queryParameters.put("world_name", worldName);
        queryParameters.put("ocid", ocid);

        return request(
                RANKING_OVERALL_PATH,
                queryParameters,
                CharacterRankingResponse.class,
                CHARACTER_WORLD_RANKING_API,
                "ocid",
                ocid
        );
    }

    @Override
    public CharacterRankingResponse getWorldClassRanking(
            String ocid,
            String worldName,
            String classRankingFilter,
            LocalDate date
    ) {
        LinkedHashMap<String, String> queryParameters = new LinkedHashMap<>();

        queryParameters.put("date", date.toString());
        queryParameters.put("world_name", worldName);
        queryParameters.put("class", classRankingFilter);
        queryParameters.put("ocid", ocid);

        return request(
                RANKING_OVERALL_PATH,
                queryParameters,
                CharacterRankingResponse.class,
                CHARACTER_WORLD_CLASS_RANKING_API,
                "ocid",
                ocid
        );
    }

    @Override
    public CharacterRankingResponse getClassRanking(
            String ocid,
            String classRankingFilter,
            LocalDate date
    ) {
        LinkedHashMap<String, String> queryParameters =
                new LinkedHashMap<>();

        queryParameters.put("date", date.toString());
        queryParameters.put("class", classRankingFilter);
        queryParameters.put("ocid", ocid);

        return request(
                RANKING_OVERALL_PATH,
                queryParameters,
                CharacterRankingResponse.class,
                CHARACTER_CLASS_RANKING_API,
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
        return request(
                path,
                queryParameters,
                responseType,
                apiName,
                identifierName,
                identifierValue,
                0
        );
    }

    private <T> T request(
            String path,
            Map<String, String> queryParameters,
            Class<T> responseType,
            String apiName,
            String identifierName,
            String identifierValue,
            int rateLimitRetryCount
    ) {
        String logIdentifierValue =
                maskIdentifierValue(
                        identifierName,
                        identifierValue
                );

        try {
            T response = restClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path(path);

                        queryParameters.forEach(
                                (name, value) -> uriBuilder.queryParam(name, value)
                        );

                        return uriBuilder.build();
                    })
                    .retrieve()
                    .body(responseType);

            if (response == null) {
                log.warn(
                        "넥슨 API 응답 본문이 없습니다. api={}, {}={}",
                        apiName,
                        identifierName,
                        logIdentifierValue
                );

                throw new CharacterException(
                        CharacterErrorCode.NEXON_API_RESPONSE_INVALID
                );
            }

            return response;

        } catch (HttpClientErrorException.NotFound exception) {
            NexonApiErrorResponse.NexonApiError nexonError =
                    getNexonError(
                            apiName,
                            exception
                    );

            log.info(
                    "넥슨 API 조회 결과가 없습니다. "
                            + "api={}, status={}, nexonErrorCode={}, "
                            + "nexonErrorMessage={}, {}={}",
                    apiName,
                    exception.getStatusCode(),
                    getNexonErrorCode(nexonError),
                    getLogNexonErrorMessage(
                            nexonError,
                            identifierName,
                            identifierValue
                    ),
                    identifierName,
                    logIdentifierValue
            );

            throw new CharacterException(
                    CharacterErrorCode.CHARACTER_NOT_FOUND
            );

        } catch (HttpClientErrorException exception) {
            NexonApiErrorResponse.NexonApiError nexonError =
                    getNexonError(
                            apiName,
                            exception
                    );

            if (shouldRetryRateLimit(
                    exception,
                    nexonError,
                    rateLimitRetryCount
            )) {
                long retryDelayMillis =
                        getRateLimitRetryDelayMillis(
                                rateLimitRetryCount
                        );

                log.warn(
                        "넥슨 API 요청 제한 응답으로 재시도합니다. "
                                + "api={}, status={}, nexonErrorCode={}, "
                                + "nexonErrorMessage={}, {}={}, "
                                + "retryCount={}, retryDelayMillis={}",
                        apiName,
                        exception.getStatusCode(),
                        getNexonErrorCode(nexonError),
                        getLogNexonErrorMessage(
                                nexonError,
                                identifierName,
                                identifierValue
                        ),
                        identifierName,
                        logIdentifierValue,
                        rateLimitRetryCount + 1,
                        retryDelayMillis
                );

                sleepBeforeRetry(retryDelayMillis);

                return request(
                        path,
                        queryParameters,
                        responseType,
                        apiName,
                        identifierName,
                        identifierValue,
                        rateLimitRetryCount + 1
                );
            }

            throw convertClientErrorException(
                    exception,
                    apiName,
                    identifierName,
                    identifierValue,
                    nexonError
            );

        } catch (HttpServerErrorException exception) {
            log.error(
                    "넥슨 API 서버 오류가 발생했습니다. "
                            + "api={}, status={}, {}={}, exceptionType={}",
                    apiName,
                    exception.getStatusCode(),
                    identifierName,
                    logIdentifierValue,
                    exception.getClass().getSimpleName()
            );

            throw new CharacterException(
                    CharacterErrorCode.NEXON_API_SERVER_ERROR
            );

        } catch (ResourceAccessException exception) {
            throw convertResourceAccessException(
                    exception,
                    apiName,
                    identifierName,
                    identifierValue
            );

        } catch (RestClientException exception) {
            log.error(
                    "넥슨 API 응답 처리에 실패했습니다. "
                            + "api={}, {}={}, exceptionType={}",
                    apiName,
                    identifierName,
                    logIdentifierValue,
                    exception.getClass().getSimpleName()
            );

            throw new CharacterException(
                    CharacterErrorCode.NEXON_API_RESPONSE_INVALID
            );
        }
    }

    private CharacterException convertClientErrorException(
            HttpClientErrorException exception,
            String apiName,
            String identifierName,
            String identifierValue
    ) {
        NexonApiErrorResponse.NexonApiError nexonError =
                getNexonError(
                        apiName,
                        exception
                );

        return convertClientErrorException(
                exception,
                apiName,
                identifierName,
                identifierValue,
                nexonError
        );
    }

    private CharacterException convertClientErrorException(
            HttpClientErrorException exception,
            String apiName,
            String identifierName,
            String identifierValue,
            NexonApiErrorResponse.NexonApiError nexonError
    ) {

        String nexonErrorCode =
                getNexonErrorCode(nexonError);

        String nexonErrorMessage =
                getLogNexonErrorMessage(
                        nexonError,
                        identifierName,
                        identifierValue
                );

        String logIdentifierValue =
                maskIdentifierValue(
                        identifierName,
                        identifierValue
                );

        if (isCharacterNotFound(
                apiName,
                nexonErrorCode
        )) {
            log.info(
                    "넥슨 API 캐릭터 조회 결과가 없습니다. "
                            + "api={}, status={}, nexonErrorCode={}, "
                            + "nexonErrorMessage={}, {}={}",
                    apiName,
                    exception.getStatusCode(),
                    nexonErrorCode,
                    nexonErrorMessage,
                    identifierName,
                    logIdentifierValue
            );

            return new CharacterException(
                    CharacterErrorCode.CHARACTER_NOT_FOUND
            );
        }

        log.warn(
                "넥슨 API 요청 오류가 발생했습니다. "
                        + "api={}, status={}, nexonErrorCode={}, "
                        + "nexonErrorMessage={}, {}={}",
                apiName,
                exception.getStatusCode(),
                nexonErrorCode,
                nexonErrorMessage,
                identifierName,
                logIdentifierValue
        );

        return new CharacterException(
                CharacterErrorCode.NEXON_API_CLIENT_ERROR
        );
    }

    private boolean shouldRetryRateLimit(
            HttpClientErrorException exception,
            NexonApiErrorResponse.NexonApiError nexonError,
            int rateLimitRetryCount
    ) {
        if (rateLimitRetryCount >= MAX_RATE_LIMIT_RETRY_COUNT) {
            return false;
        }

        return exception.getStatusCode().value() == 429
                && RATE_LIMIT_ERROR_CODE.equals(
                getNexonErrorCode(nexonError)
        );
    }

    private long getRateLimitRetryDelayMillis(
            int rateLimitRetryCount
    ) {
        return RATE_LIMIT_RETRY_DELAY_MILLIS
                * (rateLimitRetryCount + 1);
    }

    private void sleepBeforeRetry(
            long retryDelayMillis
    ) {
        try {
            Thread.sleep(retryDelayMillis);
        } catch (InterruptedException exception) {
            Thread.currentThread()
                    .interrupt();

            throw new CharacterException(
                    CharacterErrorCode.NEXON_API_SERVER_ERROR
            );
        }
    }

    private CharacterException convertResourceAccessException(
            ResourceAccessException exception,
            String apiName,
            String identifierName,
            String identifierValue
    ) {
        String logIdentifierValue =
                maskIdentifierValue(
                        identifierName,
                        identifierValue
                );

        if (isTimeout(exception)) {
            log.error(
                    "넥슨 API 응답 시간이 초과되었습니다. "
                            + "api={}, {}={}, exceptionType={}",
                    apiName,
                    identifierName,
                    logIdentifierValue,
                    exception.getClass().getSimpleName()
            );

            return new CharacterException(
                    CharacterErrorCode.NEXON_API_TIMEOUT
            );
        }

        log.error(
                "넥슨 API 통신에 실패했습니다. "
                        + "api={}, {}={}, exceptionType={}",
                apiName,
                identifierName,
                logIdentifierValue,
                exception.getClass().getSimpleName()
        );

        return new CharacterException(
                CharacterErrorCode.NEXON_API_SERVER_ERROR
        );
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

    private NexonApiErrorResponse.NexonApiError getNexonError(
            String apiName,
            HttpClientErrorException exception
    ) {
        try {
            NexonApiErrorResponse errorResponse =
                    objectMapper.readValue(
                            exception.getResponseBodyAsByteArray(),
                            NexonApiErrorResponse.class
                    );

            if (errorResponse == null
                    || errorResponse.error() == null
                    || !StringUtils.hasText(
                    errorResponse.error().name()
            )) {
                log.warn(
                        "넥슨 API 오류 응답에 오류 코드가 없습니다. "
                                + "api={}, status={}",
                        apiName,
                        exception.getStatusCode()
                );

                return null;
            }

            return errorResponse.error();

        } catch (IOException parsingException) {
            log.warn(
                    "넥슨 API 오류 응답 파싱에 실패했습니다. "
                            + "api={}, status={}, exceptionType={}",
                    apiName,
                    exception.getStatusCode(),
                    parsingException.getClass().getSimpleName()
            );

            return null;
        }
    }

    private String getNexonErrorCode(
            NexonApiErrorResponse.NexonApiError nexonError
    ) {
        return nexonError == null
                ? null
                : nexonError.name();
    }

    private String getNexonErrorMessage(
            NexonApiErrorResponse.NexonApiError nexonError
    ) {
        return nexonError == null
                ? null
                : nexonError.message();
    }

    private String getLogNexonErrorMessage(
            NexonApiErrorResponse.NexonApiError nexonError,
            String identifierName,
            String identifierValue
    ) {
        String nexonErrorMessage =
                getNexonErrorMessage(nexonError);

        if (!"ocid".equals(identifierName)
                || !StringUtils.hasText(identifierValue)
                || !StringUtils.hasText(nexonErrorMessage)) {
            return nexonErrorMessage;
        }

        return nexonErrorMessage.replace(
                identifierValue,
                maskIdentifierValue(
                        identifierName,
                        identifierValue
                )
        );
    }

    private String maskIdentifierValue(
            String identifierName,
            String identifierValue
    ) {
        if (!"ocid".equals(identifierName)
                || !StringUtils.hasText(identifierValue)) {
            return identifierValue;
        }

        if (identifierValue.length() <= 8) {
            return "***";
        }

        return identifierValue.substring(0, 4)
                + "..."
                + identifierValue.substring(
                        identifierValue.length() - 4
                );
    }

    private boolean isTimeout(
            Throwable throwable
    ) {
        Throwable cause = throwable;

        while (cause != null) {
            if (cause instanceof HttpTimeoutException
                    || cause instanceof SocketTimeoutException) {
                return true;
            }

            cause = cause.getCause();
        }

        return false;
    }
}
