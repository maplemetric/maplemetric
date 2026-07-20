package com.maplemetric.character.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maplemetric.character.domain.exception.CharacterErrorCode;
import com.maplemetric.character.domain.exception.CharacterException;
import com.maplemetric.character.infrastructure.client.dto.CharacterSymbolResponse;
import com.maplemetric.character.infrastructure.client.dto.CharacterUnionResponse;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.web.client.RestClient;

class CharacterClientImplTest {

    private static final String BASE_URL =
            "https://open.api.nexon.com";

    private static final String CHARACTER_NAME =
            "test1234";

    private static final String OCID =
            "test-ocid";

    private static final String CHARACTER_OCID_PATH =
            "/maplestory/v1/id";

    private static final String CHARACTER_BASIC_PATH =
            "/maplestory/v1/character/basic";

    private static final String CHARACTER_UNION_PATH =
            "/maplestory/v1/user/union";

    private static final String CHARACTER_SYMBOL_PATH =
            "/maplestory/v1/character/symbol-equipment";

    private ObjectMapper objectMapper;
    private MockRestServiceServer mockServer;
    private CharacterClientImpl characterClient;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        RestClient.Builder restClientBuilder =
                RestClient.builder()
                        .baseUrl(BASE_URL);

        mockServer =
                MockRestServiceServer
                        .bindTo(restClientBuilder)
                        .build();

        characterClient = new CharacterClientImpl(
                restClientBuilder.build(),
                objectMapper
        );
    }

    @Test
    void OCID를조회한다() {
        expectGetRequest(
                CHARACTER_OCID_PATH,
                "character_name",
                CHARACTER_NAME,
                withSuccess(
                        """
                        {
                          "ocid": "test-ocid"
                        }
                        """,
                        MediaType.APPLICATION_JSON
                )
        );

        String result =
                characterClient.getOcid(CHARACTER_NAME);

        assertThat(result).isEqualTo(OCID);

        mockServer.verify();
    }

    @Test
    void 존재하지않는캐릭터는미조회예외로변환한다() {
        expectGetRequest(
                CHARACTER_OCID_PATH,
                "character_name",
                CHARACTER_NAME,
                withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(
                                """
                                {
                                  "error": {
                                    "name": "OPENAPI00004",
                                    "message": "파라미터가 유효하지 않습니다."
                                  }
                                }
                                """
                        )
        );

        CharacterException exception =
                catchThrowableOfType(
                        () -> characterClient.getOcid(
                                CHARACTER_NAME
                        ),
                        CharacterException.class
                );

        assertThat(exception.getErrorCode())
                .isEqualTo(
                        CharacterErrorCode.CHARACTER_NOT_FOUND
                );

        mockServer.verify();
    }

    @Test
    void 유효하지않은식별자는미조회예외로변환한다() {
        expectGetRequest(
                CHARACTER_OCID_PATH,
                "character_name",
                CHARACTER_NAME,
                withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(
                                """
                                {
                                  "error": {
                                    "name": "OPENAPI00003",
                                    "message": "유효하지 않은 식별자입니다."
                                  }
                                }
                                """
                        )
        );

        CharacterException exception =
                catchThrowableOfType(
                        () -> characterClient.getOcid(
                                CHARACTER_NAME
                        ),
                        CharacterException.class
                );

        assertThat(exception.getErrorCode())
                .isEqualTo(
                        CharacterErrorCode.CHARACTER_NOT_FOUND
                );

        mockServer.verify();
    }

    @Test
    void 일반4xx응답은클라이언트오류로변환한다() {
        expectGetRequest(
                CHARACTER_OCID_PATH,
                "character_name",
                CHARACTER_NAME,
                withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(
                                """
                                {
                                  "error": {
                                    "name": "OPENAPI00002",
                                    "message": "권한이 없습니다."
                                  }
                                }
                                """
                        )
        );

        CharacterException exception =
                catchThrowableOfType(
                        () -> characterClient.getOcid(
                                CHARACTER_NAME
                        ),
                        CharacterException.class
                );

        assertThat(exception.getErrorCode())
                .isEqualTo(
                        CharacterErrorCode.NEXON_API_CLIENT_ERROR
                );

        mockServer.verify();
    }

    @Test
    void 유니온정보를조회한다() {
        expectGetRequest(
                CHARACTER_UNION_PATH,
                "ocid",
                OCID,
                withSuccess(
                        """
                        {
                          "union_level": 9000,
                          "union_artifact_level": 50
                        }
                        """,
                        MediaType.APPLICATION_JSON
                )
        );

        CharacterUnionResponse result =
                characterClient.getCharacterUnion(OCID);

        assertThat(result.unionLevel())
                .isEqualTo(9000);

        assertThat(result.unionArtifactLevel())
                .isEqualTo(50);

        mockServer.verify();
    }

    @Test
    void 장착심볼정보를조회한다() {
        expectGetRequest(
                CHARACTER_SYMBOL_PATH,
                "ocid",
                OCID,
                withSuccess(
                        """
                        {
                          "date": null,
                          "character_class": "팬텀",
                          "symbol": [
                            {
                              "symbol_name": "아케인심볼 : 소멸의 여로",
                              "symbol_level": 20
                            },
                            {
                              "symbol_name": "그랜드 어센틱심볼 : 탈라하트",
                              "symbol_level": 5
                            }
                          ]
                        }
                        """,
                        MediaType.APPLICATION_JSON
                )
        );

        CharacterSymbolResponse result =
                characterClient.getCharacterSymbol(OCID);

        assertThat(result.characterClass())
                .isEqualTo("팬텀");

        assertThat(result.symbol())
                .hasSize(2);

        assertThat(result.symbol())
                .extracting(
                        symbol -> symbol.symbolName(),
                        symbol -> symbol.symbolLevel()
                )
                .containsExactly(
                        tuple("아케인심볼 : 소멸의 여로", 20),
                        tuple("그랜드 어센틱심볼 : 탈라하트", 5)
                );

        mockServer.verify();
    }

    @Test
    void 오류응답파싱에실패하면클라이언트오류로변환한다() {
        expectGetRequest(
                CHARACTER_OCID_PATH,
                "character_name",
                CHARACTER_NAME,
                withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.TEXT_PLAIN)
                        .body("invalid-error-response")
        );

        CharacterException exception =
                catchThrowableOfType(
                        () -> characterClient.getOcid(
                                CHARACTER_NAME
                        ),
                        CharacterException.class
                );

        assertThat(exception.getErrorCode())
                .isEqualTo(
                        CharacterErrorCode.NEXON_API_CLIENT_ERROR
                );

        mockServer.verify();
    }

    @Test
    void 서버5xx응답은서버오류로변환한다() {
        expectGetRequest(
                CHARACTER_BASIC_PATH,
                "ocid",
                OCID,
                withStatus(
                        HttpStatus.INTERNAL_SERVER_ERROR
                )
        );

        CharacterException exception =
                catchThrowableOfType(
                        () -> characterClient
                                .getCharacterBasic(OCID),
                        CharacterException.class
                );

        assertThat(exception.getErrorCode())
                .isEqualTo(
                        CharacterErrorCode.NEXON_API_SERVER_ERROR
                );

        mockServer.verify();
    }

    @Test
    void 응답본문이없으면잘못된응답오류를반환한다() {
        expectGetRequest(
                CHARACTER_OCID_PATH,
                "character_name",
                CHARACTER_NAME,
                withStatus(HttpStatus.OK)
        );

        CharacterException exception =
                catchThrowableOfType(
                        () -> characterClient.getOcid(
                                CHARACTER_NAME
                        ),
                        CharacterException.class
                );

        assertThat(exception.getErrorCode())
                .isEqualTo(
                        CharacterErrorCode.NEXON_API_RESPONSE_INVALID
                );

        mockServer.verify();
    }

    @Test
    void OCID가비어있으면잘못된응답오류를반환한다() {
        expectGetRequest(
                CHARACTER_OCID_PATH,
                "character_name",
                CHARACTER_NAME,
                withSuccess(
                        """
                        {
                          "ocid": ""
                        }
                        """,
                        MediaType.APPLICATION_JSON
                )
        );

        CharacterException exception =
                catchThrowableOfType(
                        () -> characterClient.getOcid(
                                CHARACTER_NAME
                        ),
                        CharacterException.class
                );

        assertThat(exception.getErrorCode())
                .isEqualTo(
                        CharacterErrorCode.NEXON_API_RESPONSE_INVALID
                );

        mockServer.verify();
    }

    @Test
    void OCID가누락되면잘못된응답오류를반환한다() {
        expectGetRequest(
                CHARACTER_OCID_PATH,
                "character_name",
                CHARACTER_NAME,
                withSuccess(
                        "{}",
                        MediaType.APPLICATION_JSON
                )
        );

        CharacterException exception =
                catchThrowableOfType(
                        () -> characterClient.getOcid(
                                CHARACTER_NAME
                        ),
                        CharacterException.class
                );

        assertThat(exception.getErrorCode())
                .isEqualTo(
                        CharacterErrorCode.NEXON_API_RESPONSE_INVALID
                );

        mockServer.verify();
    }

    @Test
    void 타임아웃은타임아웃오류로변환한다() {
        CharacterClientImpl timeoutClient =
                createFailingClient(
                        new SocketTimeoutException(
                                "read timeout"
                        )
                );

        CharacterException exception =
                catchThrowableOfType(
                        () -> timeoutClient.getOcid(
                                CHARACTER_NAME
                        ),
                        CharacterException.class
                );

        assertThat(exception.getErrorCode())
                .isEqualTo(
                        CharacterErrorCode.NEXON_API_TIMEOUT
                );
    }

    @Test
    void 일반통신오류는서버오류로변환한다() {
        CharacterClientImpl connectionFailureClient =
                createFailingClient(
                        new ConnectException(
                                "connection refused"
                        )
                );

        CharacterException exception =
                catchThrowableOfType(
                        () -> connectionFailureClient.getOcid(
                                CHARACTER_NAME
                        ),
                        CharacterException.class
                );

        assertThat(exception.getErrorCode())
                .isEqualTo(
                        CharacterErrorCode.NEXON_API_SERVER_ERROR
                );
    }

    private void expectGetRequest(
            String path,
            String queryParameterName,
            String queryParameterValue,
            ResponseCreator responseCreator
    ) {
        mockServer.expect(request -> {
                    assertThat(request.getMethod())
                            .isEqualTo(HttpMethod.GET);

                    assertThat(request.getURI().getPath())
                            .isEqualTo(path);

                    assertThat(request.getURI().getQuery())
                            .isEqualTo(
                                    queryParameterName
                                            + "="
                                            + queryParameterValue
                            );
                })
                .andRespond(responseCreator);
    }

    private CharacterClientImpl createFailingClient(
            IOException exception
    ) {
        ClientHttpRequestFactory requestFactory =
                (uri, httpMethod) -> {
                    throw exception;
                };

        RestClient restClient =
                RestClient.builder()
                        .baseUrl(BASE_URL)
                        .requestFactory(requestFactory)
                        .build();

        return new CharacterClientImpl(
                restClient,
                objectMapper
        );
    }
}
