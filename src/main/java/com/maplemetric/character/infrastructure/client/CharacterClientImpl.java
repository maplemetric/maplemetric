package com.maplemetric.character.infrastructure.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maplemetric.character.domain.exception.CharacterErrorCode;
import com.maplemetric.character.domain.exception.CharacterException;
import com.maplemetric.character.infrastructure.client.dto.CharacterBasicResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterEquipmentResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterSymbolResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterStatResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterUnionResponse;
import com.maplemetric.character.infrastructure.client.dto.NexonApiErrorResponse;
import com.maplemetric.character.infrastructure.client.dto.OcidResponse;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
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

    private static final String CHARACTER_OCID_API = "캐릭터 식별자";

    private static final String CHARACTER_BASIC_API = "캐릭터 기본 정보";

    private static final String CHARACTER_EQUIPMENT_API = "캐릭터 장비 정보";

    private static final String CHARACTER_STAT_API = "캐릭터 스탯 정보";

    private static final String CHARACTER_UNION_API = "캐릭터 유니온 정보";

    private static final String CHARACTER_SYMBOL_API = "캐릭터 장착 심볼 정보";

    private static final String INVALID_IDENTIFIER_ERROR_CODE = "OPENAPI00003";

    private static final String INVALID_PARAMETER_ERROR_CODE = "OPENAPI00004";

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

    private <T> T request(
            String path,
            String queryParameterName,
            String queryParameterValue,
            Class<T> responseType,
            String apiName,
            String identifierName,
            String identifierValue
    ) {
        try {
            T response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(path)
                            .queryParam(
                                    queryParameterName,
                                    queryParameterValue
                            )
                            .build()
                    )
                    .retrieve()
                    .body(responseType);

            if (response == null) {
                log.warn(
                        "넥슨 API 응답 본문이 없습니다. api={}, {}={}",
                        apiName,
                        identifierName,
                        identifierValue
                );

                throw new CharacterException(
                        CharacterErrorCode.NEXON_API_RESPONSE_INVALID
                );
            }

            return response;

        } catch (HttpClientErrorException.NotFound exception) {
            log.info(
                    "넥슨 API 조회 결과가 없습니다. api={}, {}={}",
                    apiName,
                    identifierName,
                    identifierValue
            );

            throw new CharacterException(
                    CharacterErrorCode.CHARACTER_NOT_FOUND
            );

        } catch (HttpClientErrorException exception) {
            throw convertClientErrorException(
                    exception,
                    apiName,
                    identifierName,
                    identifierValue
            );

        } catch (HttpServerErrorException exception) {
            log.error(
                    "넥슨 API 서버 오류가 발생했습니다. "
                            + "api={}, status={}, {}={}",
                    apiName,
                    exception.getStatusCode(),
                    identifierName,
                    identifierValue,
                    exception
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
                    "넥슨 API 응답 처리에 실패했습니다. api={}, {}={}",
                    apiName,
                    identifierName,
                    identifierValue,
                    exception
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
        String nexonErrorCode = getNexonErrorCode(
                apiName,
                exception
        );

        if (isCharacterNotFound(
                apiName,
                nexonErrorCode
        )) {
            log.info(
                    "넥슨 API 캐릭터 조회 결과가 없습니다. "
                            + "characterName={}, nexonErrorCode={}",
                    identifierValue,
                    nexonErrorCode
            );

            return new CharacterException(
                    CharacterErrorCode.CHARACTER_NOT_FOUND
            );
        }

        log.warn(
                "넥슨 API 요청 오류가 발생했습니다. "
                        + "api={}, status={}, nexonErrorCode={}, {}={}",
                apiName,
                exception.getStatusCode(),
                nexonErrorCode,
                identifierName,
                identifierValue
        );

        return new CharacterException(
                CharacterErrorCode.NEXON_API_CLIENT_ERROR
        );
    }

    private CharacterException convertResourceAccessException(
            ResourceAccessException exception,
            String apiName,
            String identifierName,
            String identifierValue
    ) {
        if (isTimeout(exception)) {
            log.error(
                    "넥슨 API 응답 시간이 초과되었습니다. api={}, {}={}",
                    apiName,
                    identifierName,
                    identifierValue,
                    exception
            );

            return new CharacterException(
                    CharacterErrorCode.NEXON_API_TIMEOUT
            );
        }

        log.error(
                "넥슨 API 통신에 실패했습니다. api={}, {}={}",
                apiName,
                identifierName,
                identifierValue,
                exception
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

    private String getNexonErrorCode(
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

            return errorResponse.error().name();

        } catch (IOException parsingException) {
            log.warn(
                    "넥슨 API 오류 응답 파싱에 실패했습니다. "
                            + "api={}, status={}",
                    apiName,
                    exception.getStatusCode(),
                    parsingException
            );

            return null;
        }
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
